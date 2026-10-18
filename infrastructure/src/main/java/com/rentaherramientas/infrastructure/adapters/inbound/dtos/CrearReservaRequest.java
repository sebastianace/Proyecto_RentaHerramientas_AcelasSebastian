package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.CrearReservaCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Solicitud de reserva: fechas y herramientas (todas del mismo proveedor)")
public record CrearReservaRequest(
        @Schema(example = "2026-10-20")
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @Schema(example = "2026-10-22")
        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        @NotEmpty(message = "Debes incluir al menos una herramienta")
        @Size(max = 20, message = "Máximo 20 herramientas por reserva")
        List<@Valid Item> herramientas,

        @Schema(example = "Entregar en la portería de la obra")
        @Size(max = 500)
        String notas) {

    public record Item(
            @Schema(example = "1")
            @NotNull(message = "El id de la herramienta es obligatorio")
            Long herramientaId,

            @Schema(example = "2")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 100)
            int cantidad) {
    }

    public CrearReservaCommand aCommand() {
        return new CrearReservaCommand(fechaInicio, fechaFin,
                herramientas.stream().map(i -> new CrearReservaCommand.Item(i.herramientaId(), i.cantidad())).toList(),
                notas);
    }
}
