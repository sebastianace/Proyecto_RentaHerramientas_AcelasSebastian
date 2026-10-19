package com.rentaherramientas.application.dtos;

public record ProveedorResult(
        Long id,
        UsuarioResult usuario,
        String razonSocial,
        String nit,
        String direccion,
        String ciudad,
        String descripcion) {
}
