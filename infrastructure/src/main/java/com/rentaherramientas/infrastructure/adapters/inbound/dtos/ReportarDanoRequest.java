package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.ReportarDanoCommand;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Daño encontrado en una herramienta devuelta. GRAVE saca de servicio una unidad")
public record ReportarDanoRequest(
        @Schema(example = "1")
        @NotNull(message = "La herramienta es obligatoria")
        Long herramientaId,

        @Schema(example = "Mandril con juego excesivo; no sujeta la broca")
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500)
        String descripcion,

        @Schema(example = "MODERADO")
        @NotNull(message = "La severidad es obligatoria")
        SeveridadDano severidad,

        @Schema(example = "85000")
        @PositiveOrZero(message = "El costo no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal costoEstimado) {

    public ReportarDanoCommand aCommand() {
        return new ReportarDanoCommand(herramientaId, descripcion, severidad, costoEstimado);
    }
}
