package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;

import java.util.List;

/**
 * Confirmación de la devolución. Los daños se pueden reportar en la misma petición (lo normal al revisar
 * el equipo) o después, uno a uno.
 */
public record ConfirmarDevolucionCommand(EstadoEquipo estadoEquipo, String observaciones,
                                         List<ReportarDanoCommand> danos) {

    public ConfirmarDevolucionCommand {
        danos = danos == null ? List.of() : List.copyOf(danos);
    }
}
