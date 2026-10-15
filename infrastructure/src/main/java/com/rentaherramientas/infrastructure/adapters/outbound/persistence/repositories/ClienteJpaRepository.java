package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.TipoDocumento;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ClienteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    /** @EntityGraph trae el usuario en la misma consulta (JOIN) en vez de una consulta por cliente. */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<ClienteEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    Optional<ClienteEntity> findByUsuario_Id(Long usuarioId);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
