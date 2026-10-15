package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.CrearHerramientaCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos para agregar una herramienta al inventario")
public record CrearHerramientaRequest(
        @Schema(description = "Solo para ADMIN: proveedor dueño de la herramienta. Un PROVEEDOR no lo envía", example = "1")
        Long proveedorId,

        @Schema(example = "1")
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Schema(example = "Taladro percutor 13 mm")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @Schema(example = "Taladro percutor de 750 W con maletín y juego de brocas")
        @Size(max = 5000)
        String descripcion,

        @Schema(example = "Bosch")
        @Size(max = 60)
        String marca,

        @Schema(example = "GSB 13 RE")
        @Size(max = 60)
        String modelo,

        @Schema(example = "28000")
        @NotNull(message = "El precio por día es obligatorio")
        @DecimalMin(value = "1", message = "El precio por día debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo 10 enteros y 2 decimales")
        BigDecimal precioPorDia,

        @Schema(example = "5")
        @Min(value = 1, message = "El stock debe ser al menos 1")
        @Max(value = 10000)
        int stockTotal) {

    public CrearHerramientaCommand aCommand() {
        return new CrearHerramientaCommand(proveedorId, categoriaId, nombre, descripcion, marca, modelo,
                precioPorDia, stockTotal);
    }
}
