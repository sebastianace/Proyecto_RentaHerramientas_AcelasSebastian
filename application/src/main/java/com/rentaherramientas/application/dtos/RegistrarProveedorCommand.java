package com.rentaherramientas.application.dtos;

public record RegistrarProveedorCommand(
        String email,
        String password,
        String nombres,
        String apellidos,
        String telefono,
        String razonSocial,
        String nit,
        String direccion,
        String ciudad,
        String descripcion) {
}
