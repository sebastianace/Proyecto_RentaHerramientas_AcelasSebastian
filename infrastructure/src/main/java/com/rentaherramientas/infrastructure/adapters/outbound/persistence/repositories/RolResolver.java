package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.RolEntity;
import org.springframework.stereotype.Component;

/** Obtiene la entidad de un rol existente. Si falta en la tabla roles es un error de datos, no del usuario. */
@Component
class RolResolver {

    private final RolJpaRepository rolJpaRepository;

    RolResolver(RolJpaRepository rolJpaRepository) {
        this.rolJpaRepository = rolJpaRepository;
    }

    RolEntity buscar(RolNombre nombre) {
        return rolJpaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("El rol " + nombre + " no existe en la tabla roles"));
    }
}
