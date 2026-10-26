package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.TipoDocumento;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ClienteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    /** @EntityGraph trae el usuario en la misma consulta (JOIN) en vez de una consulta por cliente. */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<ClienteEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    Optional<ClienteEntity> findByUsuario_Id(Long usuarioId);

    /**
     * Búsqueda con filtros opcionales: si el parámetro es null, esa condición no filtra.
     * El texto se busca (sin distinguir mayúsculas) en nombres, apellidos, correo y número de documento.
     */
    @EntityGraph(attributePaths = "usuario")
    @Query(value = """
            select c from ClienteEntity c join c.usuario u
            where (:texto is null
                   or lower(u.nombres) like lower(concat('%', :texto, '%'))
                   or lower(u.apellidos) like lower(concat('%', :texto, '%'))
                   or lower(u.email) like lower(concat('%', :texto, '%'))
                   or c.numeroDocumento like concat('%', :texto, '%'))
              and (:ciudad is null or lower(c.ciudad) = lower(:ciudad))
            """,
            countQuery = """
            select count(c) from ClienteEntity c join c.usuario u
            where (:texto is null
                   or lower(u.nombres) like lower(concat('%', :texto, '%'))
                   or lower(u.apellidos) like lower(concat('%', :texto, '%'))
                   or lower(u.email) like lower(concat('%', :texto, '%'))
                   or c.numeroDocumento like concat('%', :texto, '%'))
              and (:ciudad is null or lower(c.ciudad) = lower(:ciudad))
            """)
    Page<ClienteEntity> buscar(@Param("texto") String texto, @Param("ciudad") String ciudad, Pageable pageable);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
