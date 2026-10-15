package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
