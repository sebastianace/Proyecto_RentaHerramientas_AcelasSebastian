package com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers;

import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.domain.model.aggregates.DetalleReserva;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaImagenEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaDetalleEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;

import java.util.List;

public final class ReservaEntityMapper {

    private ReservaEntityMapper() {
    }

    public static Reserva aDominio(ReservaEntity e) {
        List<DetalleReserva> detalles = e.getDetalles().stream()
                .map(d -> DetalleReserva.reconstruir(d.getId(), d.getHerramienta().getId(), d.getCantidad(),
                        Dinero.de(d.getPrecioPorDia()), d.getDias()))
                .toList();
        return Reserva.reconstruir(e.getId(), e.getCodigo(), e.getCliente().getId(), e.getProveedor().getId(),
                new RangoFechas(e.getFechaInicio(), e.getFechaFin()), e.getEstado(), detalles,
                e.getFechaLimitePago(), e.getMotivoRechazo(), e.getNotas());
    }

    /** Copia los datos que cambian con el ciclo de vida (los detalles solo se escriben al crear). */
    public static void copiarDatos(Reserva origen, ReservaEntity destino) {
        destino.setCodigo(origen.getCodigo());
        destino.setFechaInicio(origen.getRango().inicio());
        destino.setFechaFin(origen.getRango().fin());
        destino.setEstado(origen.getEstado());
        destino.setSubtotal(origen.getSubtotal().monto());
        destino.setIva(origen.getIva().monto());
        destino.setTotal(origen.getTotal().monto());
        destino.setFechaLimitePago(origen.getFechaLimitePago());
        destino.setMotivoRechazo(origen.getMotivoRechazo());
        destino.setNotas(origen.getNotas());
    }

    public static ReservaResult aResult(ReservaEntity e) {
        UsuarioEntity usuarioCliente = e.getCliente().getUsuario();
        List<ReservaResult.Detalle> detalles = e.getDetalles().stream().map(d -> {
            HerramientaEntity h = d.getHerramienta();
            String imagen = h.getImagenes().stream().filter(HerramientaImagenEntity::isPrincipal)
                    .map(HerramientaImagenEntity::getUrl).findFirst().orElse(null);
            return new ReservaResult.Detalle(h.getId(), h.getNombre(), imagen, d.getCantidad(), d.getPrecioPorDia(),
                    d.getDias(), d.getSubtotal());
        }).toList();
        return new ReservaResult(
                e.getId(), e.getCodigo(), e.getEstado(), e.getFechaInicio(), e.getFechaFin(),
                detalles.isEmpty() ? 0 : detalles.get(0).dias(),
                e.getSubtotal(), e.getIva(), e.getTotal(), e.getFechaLimitePago(), e.getMotivoRechazo(), e.getNotas(),
                e.getCreatedAt(),
                new ReservaResult.ClienteResumen(e.getCliente().getId(),
                        usuarioCliente.getNombres() + " " + usuarioCliente.getApellidos(),
                        usuarioCliente.getEmail(), usuarioCliente.getTelefono()),
                new ReservaResult.ProveedorResumen(e.getProveedor().getId(), e.getProveedor().getRazonSocial(),
                        e.getProveedor().getCiudad(), e.getProveedor().getUsuario().getTelefono()),
                detalles);
    }

    public static ReservaDetalleEntity nuevoDetalle(DetalleReserva detalle, ReservaEntity reserva,
                                                    HerramientaEntity herramienta) {
        ReservaDetalleEntity entidad = new ReservaDetalleEntity();
        entidad.setReserva(reserva);
        entidad.setHerramienta(herramienta);
        entidad.setCantidad(detalle.getCantidad());
        entidad.setPrecioPorDia(detalle.getPrecioPorDia().monto());
        entidad.setDias(detalle.getDias());
        entidad.setSubtotal(detalle.getSubtotal().monto());
        return entidad;
    }
}
