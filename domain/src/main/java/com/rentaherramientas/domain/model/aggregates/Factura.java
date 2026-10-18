package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoFactura;

import java.time.LocalDateTime;

/**
 * Factura de un alquiler. Se emite automáticamente cuando un pago queda APROBADO.
 * Copia los montos de la reserva al momento de emitirse: una factura es un documento
 * que no cambia aunque después cambien otros datos.
 */
public class Factura {

    private final Long id;
    private final String numero;
    private final Long pagoId;
    private final LocalDateTime fechaEmision;
    private final Dinero subtotal;
    private final Dinero iva;
    private final Dinero total;
    private EstadoFactura estado;

    private Factura(Long id, String numero, Long pagoId, LocalDateTime fechaEmision, Dinero subtotal, Dinero iva,
                    Dinero total, EstadoFactura estado) {
        if (pagoId == null || fechaEmision == null || estado == null) {
            throw new ReglaDeNegocioException("La factura requiere pago, fecha de emisión y estado");
        }
        if (!subtotal.sumar(iva).equals(total)) {
            throw new ReglaDeNegocioException("El total de la factura debe ser subtotal + IVA");
        }
        this.id = id;
        this.numero = ReglaDeNegocioException.requerido(numero, "numero");
        this.pagoId = pagoId;
        this.fechaEmision = fechaEmision;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.estado = estado;
    }

    /** Solo se factura un pago aprobado, y por exactamente los montos de su reserva. */
    public static Factura emitir(String numero, Pago pago, Reserva reserva, LocalDateTime fecha) {
        if (!pago.estaAprobado()) {
            throw new ReglaDeNegocioException("Solo se puede facturar un pago aprobado");
        }
        if (!pago.getReservaId().equals(reserva.getId())) {
            throw new ReglaDeNegocioException("El pago no corresponde a la reserva");
        }
        return new Factura(null, numero, pago.getId(), fecha, reserva.getSubtotal(), reserva.getIva(),
                reserva.getTotal(), EstadoFactura.EMITIDA);
    }

    public static Factura reconstruir(Long id, String numero, Long pagoId, LocalDateTime fechaEmision, Dinero subtotal,
                                      Dinero iva, Dinero total, EstadoFactura estado) {
        return new Factura(id, numero, pagoId, fechaEmision, subtotal, iva, total, estado);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Long getPagoId() { return pagoId; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public Dinero getSubtotal() { return subtotal; }
    public Dinero getIva() { return iva; }
    public Dinero getTotal() { return total; }
    public EstadoFactura getEstado() { return estado; }
}
