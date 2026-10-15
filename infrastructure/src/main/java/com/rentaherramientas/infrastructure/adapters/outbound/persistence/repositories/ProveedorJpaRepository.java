package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ProveedorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<ProveedorEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    Optional<ProveedorEntity> findByUsuario_Id(Long usuarioId);

    boolean existsByNit(String nit);
}
