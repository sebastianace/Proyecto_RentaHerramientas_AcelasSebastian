package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarReservaRequest(
        @Schema(example = "Las unidades están comprometidas en mantenimiento esa semana")
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(max = 300)
        String motivo) {
}
