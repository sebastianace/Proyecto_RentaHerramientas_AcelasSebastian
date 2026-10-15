package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoHerramientaRequest(
        @Schema(example = "EN_MANTENIMIENTO")
        @NotNull(message = "El estado es obligatorio")
        EstadoHerramienta estado) {
}
