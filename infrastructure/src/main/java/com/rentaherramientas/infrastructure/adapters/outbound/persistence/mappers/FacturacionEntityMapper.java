package com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers;

import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ClienteEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.FacturaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.PagoEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ProveedorEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;

import java.util.List;

/** Arma las vistas de pagos y facturas a partir de las entidades JPA. */
public final class FacturacionEntityMapper {

    private FacturacionEntityMapper() {
    }

    public static PagoResult aResult(PagoEntity p) {
        ReservaEntity r = p.getReserva();
        UsuarioEntity usuarioCliente = r.getCliente().getUsuario();
        PagoResult.FacturaResumen factura = p.getFactura() == null
                ? null
                : new PagoResult.FacturaResumen(p.getFactura().getId(), p.getFactura().getNumero());
        return new PagoResult(p.getId(), p.getMonto(), p.getMetodo(), p.getEstado(), p.getReferenciaTransaccion(),
                p.getFechaPago(),
                new PagoResult.ReservaResumen(r.getId(), r.getCodigo(), r.getEstado(), r.getCliente().getId(),
                        usuarioCliente.getNombres() + " " + usuarioCliente.getApellidos(),
                        r.getProveedor().getId(), r.getProveedor().getRazonSocial()),
                factura);
    }

    public static FacturaResult aResult(FacturaEntity f) {
        PagoEntity pago = f.getPago();
        ReservaEntity reserva = pago.getReserva();
        ProveedorEntity proveedor = reserva.getProveedor();
        ClienteEntity cliente = reserva.getCliente();
        UsuarioEntity usuarioCliente = cliente.getUsuario();
        List<FacturaResult.Linea> lineas = reserva.getDetalles().stream()
                .map(d -> new FacturaResult.Linea(d.getHerramienta().getNombre(), d.getCantidad(), d.getDias(),
                        d.getPrecioPorDia(), d.getSubtotal()))
                .toList();
        return new FacturaResult(
                f.getId(), f.getNumero(), f.getEstado(), f.getFechaEmision(), f.getSubtotal(), f.getIva(), f.getTotal(),
                new FacturaResult.Emisor(proveedor.getId(), proveedor.getRazonSocial(), proveedor.getNit(),
                        proveedor.getDireccion(), proveedor.getCiudad(), proveedor.getUsuario().getEmail(),
                        proveedor.getUsuario().getTelefono()),
                new FacturaResult.Cliente(cliente.getId(),
                        usuarioCliente.getNombres() + " " + usuarioCliente.getApellidos(),
                        cliente.getTipoDocumento(), cliente.getNumeroDocumento(), cliente.getDireccion(),
                        cliente.getCiudad(), usuarioCliente.getEmail()),
                new FacturaResult.Reserva(reserva.getId(), reserva.getCodigo(), reserva.getFechaInicio(),
                        reserva.getFechaFin()),
                new FacturaResult.Pago(pago.getId(), pago.getMetodo(), pago.getReferenciaTransaccion(),
                        pago.getFechaPago()),
                lineas);
    }
}
