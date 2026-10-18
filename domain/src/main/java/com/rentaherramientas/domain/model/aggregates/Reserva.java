package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reserva de alquiler: raíz del agregado que contiene sus líneas de detalle.
 *
 * <p>Toda transición de estado pasa por un método de negocio que valida el estado actual.
 * Así es imposible, por ejemplo, aprobar una reserva cancelada o pagar una rechazada.</p>
 */
public class Reserva {

    public static final int PORCENTAJE_IVA = 19;

    private final Long id;
    private final String codigo;
    private final Long clienteId;
    private final Long proveedorId;
    private final RangoFechas rango;
    private EstadoReserva estado;
    private final List<DetalleReserva> detalles;
    private final Dinero subtotal;
    private final Dinero iva;
    private final Dinero total;
    private LocalDateTime fechaLimitePago;
    private String motivoRechazo;
    private String notas;

    /** Lo que pide el cliente: una herramienta y cuántas unidades. */
    public record LineaSolicitada(Herramienta herramienta, int cantidad) {
    }

    private Reserva(Long id, String codigo, Long clienteId, Long proveedorId, RangoFechas rango, EstadoReserva estado,
                    List<DetalleReserva> detalles, LocalDateTime fechaLimitePago, String motivoRechazo, String notas) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ReglaDeNegocioException("La reserva debe incluir al menos una herramienta");
        }
        this.id = id;
        this.codigo = ReglaDeNegocioException.requerido(codigo, "codigo");
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.rango = rango;
        this.estado = estado;
        this.detalles = new ArrayList<>(detalles);
        this.subtotal = this.detalles.stream().map(DetalleReserva::getSubtotal).reduce(Dinero.CERO, Dinero::sumar);
        this.iva = subtotal.porcentaje(PORCENTAJE_IVA);
        this.total = subtotal.sumar(iva);
        this.fechaLimitePago = fechaLimitePago;
        this.motivoRechazo = motivoRechazo;
        this.notas = ReglaDeNegocioException.opcional(notas);
    }

    /**
     * Crea una solicitud de reserva (estado PENDIENTE) validando las reglas del negocio:
     * fechas futuras, duración máxima, herramientas reservables y todas del mismo proveedor.
     * La disponibilidad de unidades la valida el caso de uso, porque necesita consultar otras reservas.
     */
    public static Reserva solicitar(String codigo, Long clienteId, RangoFechas rango, List<LineaSolicitada> lineas,
                                    String notas, LocalDate hoy, int diasMaximosAlquiler) {
        if (clienteId == null) {
            throw new ReglaDeNegocioException("El cliente es obligatorio");
        }
        if (rango.inicio().isBefore(hoy)) {
            throw new ReglaDeNegocioException("La fecha de inicio no puede ser anterior a hoy (" + hoy + ")");
        }
        if (rango.dias() > diasMaximosAlquiler) {
            throw new ReglaDeNegocioException("El alquiler puede durar máximo " + diasMaximosAlquiler + " días");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaDeNegocioException("La reserva debe incluir al menos una herramienta");
        }
        Long proveedorId = lineas.get(0).herramienta().getProveedorId();
        Set<Long> herramientasVistas = new HashSet<>();
        List<DetalleReserva> detalles = new ArrayList<>();
        for (LineaSolicitada linea : lineas) {
            Herramienta herramienta = linea.herramienta();
            if (!herramienta.getProveedorId().equals(proveedorId)) {
                throw new ReglaDeNegocioException("Una reserva solo puede incluir herramientas de un mismo proveedor");
            }
            if (!herramientasVistas.add(herramienta.getId())) {
                throw new ReglaDeNegocioException("La herramienta '" + herramienta.getNombre()
                        + "' está repetida; usa la cantidad para pedir varias unidades");
            }
            if (!herramienta.admiteReservas()) {
                throw new ReglaDeNegocioException("La herramienta '" + herramienta.getNombre() + "' no está disponible para alquiler");
            }
            detalles.add(DetalleReserva.nuevo(herramienta.getId(), linea.cantidad(), herramienta.getPrecioPorDia(), rango.dias()));
        }
        return new Reserva(null, codigo, clienteId, proveedorId, rango, EstadoReserva.PENDIENTE, detalles, null, null, notas);
    }

    public static Reserva reconstruir(Long id, String codigo, Long clienteId, Long proveedorId, RangoFechas rango,
                                      EstadoReserva estado, List<DetalleReserva> detalles,
                                      LocalDateTime fechaLimitePago, String motivoRechazo, String notas) {
        return new Reserva(id, codigo, clienteId, proveedorId, rango, estado, detalles, fechaLimitePago,
                motivoRechazo, notas);
    }

    // ---------- Transiciones de estado ----------

    /** El proveedor acepta: las unidades quedan apartadas y el cliente tiene un plazo para pagar. */
    public void aprobar(LocalDateTime ahora, Duration plazoPago) {
        exigirEstado("aprobar", EstadoReserva.PENDIENTE);
        this.estado = EstadoReserva.APROBADA;
        this.fechaLimitePago = ahora.plus(plazoPago);
    }

    public void rechazar(String motivo) {
        exigirEstado("rechazar", EstadoReserva.PENDIENTE);
        this.motivoRechazo = ReglaDeNegocioException.requerido(motivo, "motivo");
        this.estado = EstadoReserva.RECHAZADA;
    }

    /** El cliente (o un admin) cancela antes de pagar. Después de pagar ya no se puede cancelar. */
    public void cancelar() {
        exigirEstado("cancelar", EstadoReserva.PENDIENTE, EstadoReserva.APROBADA);
        this.estado = EstadoReserva.CANCELADA;
    }

    /**
     * Verifica, SIN cambiar nada, que la reserva se pueda pagar: debe estar APROBADA y dentro del plazo.
     * Se llama antes de cobrar, para no cobrarle al cliente una reserva que no se puede confirmar.
     */
    public void verificarQueSePuedePagar(LocalDateTime ahora) {
        exigirEstado("pagar", EstadoReserva.APROBADA);
        if (estaVencida(ahora)) {
            throw new ReglaDeNegocioException("El plazo para pagar la reserva " + codigo + " venció el " + fechaLimitePago);
        }
    }

    /** Pago aprobado: la reserva queda confirmada automáticamente. */
    public void confirmarPago(LocalDateTime ahora) {
        verificarQueSePuedePagar(ahora);
        this.estado = EstadoReserva.PAGADA;
    }

    public void registrarEntrega() {
        exigirEstado("entregar", EstadoReserva.PAGADA);
        this.estado = EstadoReserva.ENTREGADA;
    }

    public void registrarDevolucion() {
        exigirEstado("registrar la devolución de", EstadoReserva.ENTREGADA);
        this.estado = EstadoReserva.DEVUELTA;
    }

    /** La marca como VENCIDA si estaba aprobada y ya pasó la fecha límite de pago. Devuelve true si cambió. */
    public boolean vencerSiCorresponde(LocalDateTime ahora) {
        if (estado == EstadoReserva.APROBADA && estaVencida(ahora)) {
            this.estado = EstadoReserva.VENCIDA;
            return true;
        }
        return false;
    }

    // ---------- Consultas ----------

    public boolean perteneceAlCliente(Long clienteId) {
        return this.clienteId.equals(clienteId);
    }

    public boolean perteneceAlProveedor(Long proveedorId) {
        return this.proveedorId.equals(proveedorId);
    }

    private boolean estaVencida(LocalDateTime ahora) {
        return fechaLimitePago != null && ahora.isAfter(fechaLimitePago);
    }

    private void exigirEstado(String accion, EstadoReserva... permitidos) {
        if (Arrays.stream(permitidos).noneMatch(e -> e == estado)) {
            throw new ReglaDeNegocioException("No se puede " + accion + " la reserva " + codigo
                    + " porque está en estado " + estado);
        }
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Long getClienteId() { return clienteId; }
    public Long getProveedorId() { return proveedorId; }
    public RangoFechas getRango() { return rango; }
    public EstadoReserva getEstado() { return estado; }
    public List<DetalleReserva> getDetalles() { return List.copyOf(detalles); }
    public Dinero getSubtotal() { return subtotal; }
    public Dinero getIva() { return iva; }
    public Dinero getTotal() { return total; }
    public LocalDateTime getFechaLimitePago() { return fechaLimitePago; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public String getNotas() { return notas; }
}
