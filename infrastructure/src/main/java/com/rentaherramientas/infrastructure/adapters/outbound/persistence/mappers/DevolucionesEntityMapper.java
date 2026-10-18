package com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers;

import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.dtos.ReservaReferencia;
import com.rentaherramientas.domain.model.aggregates.Devolucion;
import com.rentaherramientas.domain.model.aggregates.ReporteDano;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.DevolucionEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.EntregaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReporteDanoEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;

import java.util.List;

/**
 * Traduce entre las entidades JPA de entregas, devoluciones y daños y los objetos de dominio / resultados.
 * Vive en infraestructura porque conoce las entidades JPA; el dominio nunca las ve.
 */
public final class DevolucionesEntityMapper {

    private DevolucionesEntityMapper() {
    }

    // ---------- A dominio (lado de escritura) ----------

    public static Devolucion aDominio(DevolucionEntity e) {
        return Devolucion.reconstruir(e.getId(), e.getReserva().getId(), e.getFechaDevolucion(), e.getEstadoEquipo(),
                e.getObservaciones(), e.getRecibidaPor().getId(), e.isValidada(),
                e.getValidadaPor() == null ? null : e.getValidadaPor().getId(), e.getFechaValidacion());
    }

    public static ReporteDano aDominio(ReporteDanoEntity e) {
        return ReporteDano.reconstruir(e.getId(), e.getDevolucion().getId(), e.getHerramienta().getId(),
                e.getDescripcion(), e.getSeveridad(), Dinero.de(e.getCostoEstimado()), e.getEstado(),
                e.getReportadoPor().getId());
    }

    // ---------- A resultados (lado de lectura) ----------

    public static EntregaResult aResult(EntregaEntity e) {
        return new EntregaResult(e.getId(), referencia(e.getReserva()), e.getTipo(), e.getDireccionEntrega(),
                e.getFechaEntrega(), nombre(e.getRegistradaPor()), e.getObservaciones());
    }

    public static DevolucionResult aResult(DevolucionEntity e, List<ReporteDanoResult> danos) {
        return new DevolucionResult(e.getId(), referencia(e.getReserva()), e.getFechaDevolucion(), e.getEstadoEquipo(),
                e.getObservaciones(), nombre(e.getRecibidaPor()), e.isValidada(), nombre(e.getValidadaPor()),
                e.getFechaValidacion(), danos);
    }

    public static ReporteDanoResult aResult(ReporteDanoEntity e) {
        return new ReporteDanoResult(e.getId(), e.getDevolucion().getId(), referencia(e.getDevolucion().getReserva()),
                e.getHerramienta().getId(), e.getHerramienta().getNombre(), e.getDescripcion(), e.getSeveridad(),
                e.getCostoEstimado(), e.getEstado(), nombre(e.getReportadoPor()), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static ReservaReferencia referencia(ReservaEntity r) {
        return new ReservaReferencia(r.getId(), r.getCodigo(), r.getEstado(), r.getCliente().getId(),
                nombre(r.getCliente().getUsuario()), r.getProveedor().getId(), r.getProveedor().getRazonSocial());
    }

    private static String nombre(UsuarioEntity u) {
        return u == null ? null : u.getNombres() + " " + u.getApellidos();
    }
}
