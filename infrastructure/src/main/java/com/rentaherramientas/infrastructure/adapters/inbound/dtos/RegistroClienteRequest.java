package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.RegistrarClienteCommand;
import com.rentaherramientas.domain.model.valueobject.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un cliente")
public record RegistroClienteRequest(
        @Schema(example = "laura.perez@gmail.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120)
        String email,

        @Schema(example = "Clave2026", description = "Mínimo 8 caracteres, con al menos una letra y un número")
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_PASSWORD, message = ValidacionesComunes.MENSAJE_PASSWORD)
        String password,

        @Schema(example = "Laura")
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80)
        String nombres,

        @Schema(example = "Pérez Gómez")
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80)
        String apellidos,

        @Schema(example = "3001234567")
        @Pattern(regexp = ValidacionesComunes.REGEX_TELEFONO, message = ValidacionesComunes.MENSAJE_TELEFONO)
        String telefono,

        @Schema(example = "CC")
        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @Schema(example = "1098123456")
        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20)
        String numeroDocumento,

        @Schema(example = "Calle 45 # 28-10")
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 200)
        String direccion,

        @Schema(example = "Bucaramanga")
        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 80)
        String ciudad) {

    public RegistrarClienteCommand aCommand() {
        return new RegistrarClienteCommand(email, password, nombres, apellidos, telefono, tipoDocumento,
                numeroDocumento, direccion, ciudad);
    }
}
