package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Correo de la cuenta que olvidó su contraseña")
public record RecuperarPasswordRequest(
        @Schema(example = "juanpablo.ardila@gmail.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120)
        String email) {
}
