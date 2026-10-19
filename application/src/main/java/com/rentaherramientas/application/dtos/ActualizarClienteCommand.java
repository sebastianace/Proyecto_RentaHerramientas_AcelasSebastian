package com.rentaherramientas.application.dtos;

public record ActualizarClienteCommand(
        String nombres,
        String apellidos,
        String telefono,
        String direccion,
        String ciudad) {
}
