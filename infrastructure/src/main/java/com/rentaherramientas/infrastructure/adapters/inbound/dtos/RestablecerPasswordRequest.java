package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Token recibido en el enlace del correo y la contraseña nueva")
public record RestablecerPasswordRequest(
        @Schema(description = "Valor del parámetro ?token= del enlace")
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 100)
        String token,

        @Schema(example = "NuevaClave2026", description = "Mínimo 8 caracteres, con al menos una letra y un número")
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_PASSWORD, message = ValidacionesComunes.MENSAJE_PASSWORD)
        String password) {

    /** Nunca imprimir el token ni la clave (por ejemplo, en logs de depuración). */
    @Override
    public String toString() {
        return "RestablecerPasswordRequest[***]";
    }
}
