package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByNombre(RolNombre nombre);
}
