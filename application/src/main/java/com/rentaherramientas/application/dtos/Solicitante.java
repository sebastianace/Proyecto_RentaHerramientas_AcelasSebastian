package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.RolNombre;

import java.util.Set;

/**
 * Quién hace la petición (sale del token JWT). Los casos de uso lo reciben para decidir
 * si el usuario puede ver o modificar un recurso, sin depender de Spring Security.
 */
public record Solicitante(Long usuarioId, Set<RolNombre> roles) {

    public Solicitante {
        roles = Set.copyOf(roles);
    }

    public boolean esAdmin() {
        return roles.contains(RolNombre.ADMIN);
    }
}
