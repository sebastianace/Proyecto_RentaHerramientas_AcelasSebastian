package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.RegistrarProveedorCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un proveedor (lo hace el administrador)")
public record RegistroProveedorRequest(
        @Schema(example = "ventas@equiposnorte.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120)
        String email,

        @Schema(example = "Clave2026")
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_PASSWORD, message = ValidacionesComunes.MENSAJE_PASSWORD)
        String password,

        @Schema(example = "Andrés")
        @NotBlank(message = "Los nombres del contacto son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_NOMBRES)
        String nombres,

        @Schema(example = "Rojas Díaz")
        @NotBlank(message = "Los apellidos del contacto son obligatorios")
        @Pattern(regexp = ValidacionesComunes.REGEX_NOMBRE, message = ValidacionesComunes.MENSAJE_APELLIDOS)
        String apellidos,

        @Schema(example = "3109876543")
        @Pattern(regexp = ValidacionesComunes.REGEX_TELEFONO, message = ValidacionesComunes.MENSAJE_TELEFONO)
        String telefono,

        @Schema(example = "Equipos del Norte S.A.S.")
        @NotBlank(message = "La razón social es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_RAZON_SOCIAL, message = ValidacionesComunes.MENSAJE_RAZON_SOCIAL)
        String razonSocial,

        @Schema(example = "900111222-3")
        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "^\\d{9,10}-\\d$", message = "El NIT solo admite números con su dígito de verificación: 900123456-7")
        String nit,

        @Schema(example = "Cra 15 # 30-20")
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 200)
        String direccion,

        @Schema(example = "Bucaramanga")
        @NotBlank(message = "La ciudad es obligatoria")
        @Pattern(regexp = ValidacionesComunes.REGEX_CIUDAD, message = ValidacionesComunes.MENSAJE_CIUDAD)
        String ciudad,

        @Schema(example = "Alquiler de maquinaria liviana para obra civil")
        @Size(max = 500)
        String descripcion) {

    public RegistrarProveedorCommand aCommand() {
        return new RegistrarProveedorCommand(email, password, nombres, apellidos, telefono, razonSocial, nit,
                direccion, ciudad, descripcion);
    }
}
