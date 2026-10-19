package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReporteDanoResult(
        Long id,
        Long devolucionId,
        ReservaReferencia reserva,
        Long herramientaId,
        String herramienta,
        String descripcion,
        SeveridadDano severidad,
        BigDecimal costoEstimado,
        EstadoIncidencia estado,
        String reportadoPor,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
