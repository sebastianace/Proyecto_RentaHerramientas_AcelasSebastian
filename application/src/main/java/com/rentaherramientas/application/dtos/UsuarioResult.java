package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.RolNombre;

import java.util.Set;

/** Datos públicos de un usuario. Nunca incluye el hash de la clave. */
public record UsuarioResult(
        Long id,
        String email,
        String nombres,
        String apellidos,
        String telefono,
        boolean activo,
        Set<RolNombre> roles) {
}
