package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.ActualizarClienteCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos editables de un cliente. El email y el documento no se pueden cambiar")
public record ActualizarClienteRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_NOMBRES) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_APELLIDOS) String apellidos,
        @Pattern(regexp = ValidacionesComunes.REGEX_TELEFONO, message = ValidacionesComunes.MENSAJE_TELEFONO)
        String telefono,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 200) String direccion,
        @NotBlank(message = "La ciudad es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_CIUDAD, message = ValidacionesComunes.MENSAJE_CIUDAD) String ciudad) {

    public ActualizarClienteCommand aCommand() {
        return new ActualizarClienteCommand(nombres, apellidos, telefono, direccion, ciudad);
    }
}
