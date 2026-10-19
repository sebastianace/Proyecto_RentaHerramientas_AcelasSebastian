package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ArchivoGenerado;
import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.dtos.FiltroPagos;
import com.rentaherramientas.application.dtos.PagarReservaCommand;
import com.rentaherramientas.application.dtos.PagoRealizadoResult;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.application.dtos.ResultadoCobro;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.dtos.SolicitudCobro;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.in.GestionPagosUseCase;
import com.rentaherramientas.application.ports.out.ConsultaFacturasPort;
import com.rentaherramientas.application.ports.out.ConsultaPagosPort;
import com.rentaherramientas.application.ports.out.GeneradorFacturaPdfPort;
import com.rentaherramientas.application.ports.out.PasarelaPagoPort;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Factura;
import com.rentaherramientas.domain.model.aggregates.Pago;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.FacturaRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.PagoRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Pagos en línea y facturación automática.
 *
 * <p>Flujo de pago: (1) se bloquea la reserva para que un doble clic no cobre dos veces,
 * (2) se verifica que esté APROBADA y dentro del plazo ANTES de cobrar, (3) se cobra en la pasarela,
 * (4) se guarda el intento (aprobado o rechazado) y (5) si se aprobó, la reserva pasa a PAGADA
 * y se emite la factura. Los pasos 4 y 5 ocurren en la misma transacción.</p>
 */
public class GestionPagosService implements GestionPagosUseCase {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasarelaPagoPort pasarela;
    private final ConsultaPagosPort consultaPagos;
    private final ConsultaFacturasPort consultaFacturas;
    private final GeneradorFacturaPdfPort generadorPdf;
    private final Clock reloj;

    public GestionPagosService(ReservaRepository reservaRepository, PagoRepository pagoRepository,
                               FacturaRepository facturaRepository, ClienteRepository clienteRepository,
                               ProveedorRepository proveedorRepository, PasarelaPagoPort pasarela,
                               ConsultaPagosPort consultaPagos, ConsultaFacturasPort consultaFacturas,
                               GeneradorFacturaPdfPort generadorPdf, Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.pasarela = pasarela;
        this.consultaPagos = consultaPagos;
        this.consultaFacturas = consultaFacturas;
        this.generadorPdf = generadorPdf;
        this.reloj = reloj;
    }

    @Override
    public PagoRealizadoResult pagar(Long reservaId, PagarReservaCommand cmd, Solicitante solicitante) {
        Long clienteId = clienteIdDe(solicitante);
        Reserva reserva = reservaRepository.bloquearPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        if (!reserva.perteneceAlCliente(clienteId)) {
            throw new AccesoDenegadoException("Solo puedes pagar tus propias reservas");
        }
        validarMedioDePago(cmd);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        reserva.verificarQueSePuedePagar(ahora);

        ResultadoCobro cobro = pasarela.cobrar(new SolicitudCobro(reserva.getCodigo(), reserva.getTotal().monto(),
                cmd.metodo(), cmd.numeroTarjeta(), cmd.titular()));
        Pago pago = pagoRepository.guardar(Pago.registrar(reserva, cmd.metodo(), cobro.aprobado(),
                cobro.referenciaTransaccion(), ahora));

        if (pago.estaAprobado()) {
            reserva.confirmarPago(ahora);
            reservaRepository.guardar(reserva);
            String numero = facturaRepository.generarNumero(ahora.getYear());
            facturaRepository.guardar(Factura.emitir(numero, pago, reserva, ahora));
        }
        PagoResult resultado = consultaPagos.obtener(pago.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", pago.getId()));
        return new PagoRealizadoResult(pago.estaAprobado(), cobro.mensaje(), resultado);
    }

    @Override
    public Pagina<PagoResult> listarPagos(FiltroPagos filtro, SolicitudPagina solicitud, Solicitante solicitante) {
        FiltroPagos efectivo;
        if (solicitante.esAdmin()) {
            efectivo = filtro;
        } else if (solicitante.roles().contains(RolNombre.PROVEEDOR)) {
            efectivo = new FiltroPagos(filtro.estado(), null, proveedorIdDe(solicitante), filtro.desde(), filtro.hasta());
        } else {
            efectivo = new FiltroPagos(filtro.estado(), clienteIdDe(solicitante), null, filtro.desde(), filtro.hasta());
        }
        return consultaPagos.listar(efectivo, solicitud);
    }

    @Override
    public Pagina<FacturaResult> listarFacturas(SolicitudPagina solicitud, Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return consultaFacturas.listar(null, null, solicitud);
        }
        if (solicitante.roles().contains(RolNombre.PROVEEDOR)) {
            return consultaFacturas.listar(null, proveedorIdDe(solicitante), solicitud);
        }
        return consultaFacturas.listar(clienteIdDe(solicitante), null, solicitud);
    }

    @Override
    public FacturaResult obtenerFactura(Long id, Solicitante solicitante) {
        FacturaResult factura = consultaFacturas.obtener(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
        validarAccesoFactura(factura, solicitante);
        return factura;
    }

    @Override
    public ArchivoGenerado descargarFacturaPdf(Long id, Solicitante solicitante) {
        FacturaResult factura = obtenerFactura(id, solicitante);
        return new ArchivoGenerado(factura.numero() + ".pdf", "application/pdf", generadorPdf.generar(factura));
    }

    // ---------- Apoyo ----------

    private void validarMedioDePago(PagarReservaCommand cmd) {
        if (cmd.metodo() == null) {
            throw new ReglaDeNegocioException("El método de pago es obligatorio");
        }
        if (cmd.metodo().esTarjeta()) {
            String numero = cmd.numeroTarjeta() == null ? "" : cmd.numeroTarjeta().replace(" ", "");
            if (!numero.matches("\\d{13,19}")) {
                throw new ReglaDeNegocioException("Para pagar con tarjeta envía un número de 13 a 19 dígitos");
            }
        }
    }

    private void validarAccesoFactura(FacturaResult factura, Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return;
        }
        boolean esSuya = solicitante.roles().contains(RolNombre.PROVEEDOR)
                ? factura.emisor().proveedorId().equals(proveedorIdDe(solicitante))
                : factura.cliente().clienteId().equals(clienteIdDe(solicitante));
        if (!esSuya) {
            throw new AccesoDenegadoException("No tienes acceso a esta factura");
        }
    }

    private Long clienteIdDe(Solicitante solicitante) {
        return clienteRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de cliente"))
                .getId();
    }

    private Long proveedorIdDe(Solicitante solicitante) {
        return proveedorRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de proveedor"))
                .getId();
    }
}
