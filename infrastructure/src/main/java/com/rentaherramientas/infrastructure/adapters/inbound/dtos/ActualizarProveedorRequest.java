package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.ActualizarProveedorCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos editables de un proveedor. El email y el NIT no se pueden cambiar")
public record ActualizarProveedorRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_NOMBRES) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_APELLIDOS) String apellidos,
        @Pattern(regexp = ValidacionesComunes.REGEX_TELEFONO, message = ValidacionesComunes.MENSAJE_TELEFONO)
        String telefono,
        @NotBlank(message = "La razón social es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_RAZON_SOCIAL, message = ValidacionesComunes.MENSAJE_RAZON_SOCIAL) String razonSocial,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 200) String direccion,
        @NotBlank(message = "La ciudad es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_CIUDAD, message = ValidacionesComunes.MENSAJE_CIUDAD) String ciudad,
        @Size(max = 500) String descripcion) {

    public ActualizarProveedorCommand aCommand() {
        return new ActualizarProveedorCommand(nombres, apellidos, telefono, razonSocial, direccion, ciudad,
                descripcion);
    }
}
