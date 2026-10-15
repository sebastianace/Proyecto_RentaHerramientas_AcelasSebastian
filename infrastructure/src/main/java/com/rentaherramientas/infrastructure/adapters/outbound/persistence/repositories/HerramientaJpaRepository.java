package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface HerramientaJpaRepository extends JpaRepository<HerramientaEntity, Long> {

    /** Trae la herramienta con categoría y proveedor en una sola consulta (las imágenes llegan por lotes). */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    Optional<HerramientaEntity> findConRelacionesById(Long id);

    /**
     * Catálogo público. Cada filtro es opcional: "(:param is null or ...)" hace que, si no
     * llega el parámetro, esa condición no filtre nada. Solo muestra herramientas ACTIVAS
     * de proveedores cuyo usuario está activo.
     */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query(value = """
            select h from HerramientaEntity h
            join h.proveedor p
            join p.usuario u
            where h.estado = :estado
              and u.activo = true
              and (:categoriaId is null or h.categoria.id = :categoriaId)
              and (:texto is null
                   or lower(h.nombre) like lower(concat('%', :texto, '%'))
                   or lower(h.marca) like lower(concat('%', :texto, '%')))
              and (:ciudad is null or lower(p.ciudad) = lower(:ciudad))
              and (:precioMaximo is null or h.precioPorDia <= :precioMaximo)
            """,
            countQuery = """
            select count(h) from HerramientaEntity h
            join h.proveedor p
            join p.usuario u
            where h.estado = :estado
              and u.activo = true
              and (:categoriaId is null or h.categoria.id = :categoriaId)
              and (:texto is null
                   or lower(h.nombre) like lower(concat('%', :texto, '%'))
                   or lower(h.marca) like lower(concat('%', :texto, '%')))
              and (:ciudad is null or lower(p.ciudad) = lower(:ciudad))
              and (:precioMaximo is null or h.precioPorDia <= :precioMaximo)
            """)
    Page<HerramientaEntity> buscarCatalogo(@Param("estado") EstadoHerramienta estado,
                                           @Param("categoriaId") Long categoriaId,
                                           @Param("texto") String texto,
                                           @Param("ciudad") String ciudad,
                                           @Param("precioMaximo") BigDecimal precioMaximo,
                                           Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query(value = """
            select h from HerramientaEntity h
            where (:proveedorId is null or h.proveedor.id = :proveedorId)
              and (:estado is null or h.estado = :estado)
            """,
            countQuery = """
            select count(h) from HerramientaEntity h
            where (:proveedorId is null or h.proveedor.id = :proveedorId)
              and (:estado is null or h.estado = :estado)
            """)
    Page<HerramientaEntity> buscarInventario(@Param("proveedorId") Long proveedorId,
                                             @Param("estado") EstadoHerramienta estado,
                                             Pageable pageable);
}
