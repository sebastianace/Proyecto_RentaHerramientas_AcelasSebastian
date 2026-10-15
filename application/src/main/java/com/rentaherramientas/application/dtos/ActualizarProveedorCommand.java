package com.rentaherramientas.application.dtos;

public record ActualizarProveedorCommand(
        String nombres,
        String apellidos,
        String telefono,
        String razonSocial,
        String direccion,
        String ciudad,
        String descripcion) {
}
