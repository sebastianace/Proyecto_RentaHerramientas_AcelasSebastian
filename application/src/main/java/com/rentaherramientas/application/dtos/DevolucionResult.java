package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;

import java.time.LocalDateTime;
import java.util.List;

public record DevolucionResult(
        Long id,
        ReservaReferencia reserva,
        LocalDateTime fechaDevolucion,
        EstadoEquipo estadoEquipo,
        String observaciones,
        String recibidaPor,
        boolean validada,
        String validadaPor,
        LocalDateTime fechaValidacion,
        List<ReporteDanoResult> danos) {
}
