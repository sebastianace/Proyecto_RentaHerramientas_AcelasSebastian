package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.ActualizarHerramientaCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos editables de una herramienta. El proveedor dueño no se puede cambiar")
public record ActualizarHerramientaRequest(
        @NotNull(message = "La categoría es obligatoria") Long categoriaId,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @Size(max = 5000) String descripcion,
        @Size(max = 60) String marca,
        @Size(max = 60) String modelo,

        @NotNull(message = "El precio por día es obligatorio")
        @DecimalMin(value = "1", message = "El precio por día debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2)
        BigDecimal precioPorDia,

        @Schema(example = "5")
        @Min(value = 0) @Max(value = 10000)
        int stockTotal,

        @Schema(description = "Unidades dañadas o en mantenimiento (no se pueden alquilar)", example = "1")
        @Min(value = 0)
        int unidadesFueraServicio) {

    public ActualizarHerramientaCommand aCommand() {
        return new ActualizarHerramientaCommand(categoriaId, nombre, descripcion, marca, modelo, precioPorDia,
                stockTotal, unidadesFueraServicio);
    }
}
