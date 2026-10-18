package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Siguiente estado de la incidencia: ABIERTO → EN_REVISION → RESUELTO")
public record CambiarEstadoIncidenciaRequest(
        @Schema(example = "EN_REVISION")
        @NotNull(message = "El estado es obligatorio")
        EstadoIncidencia estado) {
}
