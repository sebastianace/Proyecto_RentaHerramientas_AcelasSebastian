package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Cambio de contraseña del usuario autenticado")
public record CambiarPasswordRequest(
        @Schema(example = "Renta2026*", description = "La contraseña con la que inicias sesión hoy")
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @Schema(example = "NuevaClave2026", description = "Mínimo 8 caracteres, con al menos una letra y un número")
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_PASSWORD, message = ValidacionesComunes.MENSAJE_PASSWORD)
        String passwordNueva) {

    /** Nunca imprimir las contraseñas (por ejemplo, en logs de depuración). */
    @Override
    public String toString() {
        return "CambiarPasswordRequest[***]";
    }
}
