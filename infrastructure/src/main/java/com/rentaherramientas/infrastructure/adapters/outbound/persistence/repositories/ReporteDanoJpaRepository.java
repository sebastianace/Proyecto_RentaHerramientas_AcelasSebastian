package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReporteDanoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReporteDanoJpaRepository extends JpaRepository<ReporteDanoEntity, Long> {

    @EntityGraph(attributePaths = {"devolucion", "devolucion.reserva", "devolucion.reserva.cliente",
            "devolucion.reserva.cliente.usuario", "devolucion.reserva.proveedor", "herramienta", "reportadoPor"})
    Optional<ReporteDanoEntity> findConRelacionesById(Long id);

    /** Daños de varias devoluciones en UNA consulta (evita el problema N+1 al listar devoluciones). */
    @EntityGraph(attributePaths = {"devolucion", "devolucion.reserva", "devolucion.reserva.cliente",
            "devolucion.reserva.cliente.usuario", "devolucion.reserva.proveedor", "herramienta", "reportadoPor"})
    List<ReporteDanoEntity> findByDevolucionIdInOrderByIdAsc(Collection<Long> devolucionIds);

    @EntityGraph(attributePaths = {"devolucion", "devolucion.reserva", "devolucion.reserva.cliente",
            "devolucion.reserva.cliente.usuario", "devolucion.reserva.proveedor", "herramienta", "reportadoPor"})
    @Query(value = """
            select d from ReporteDanoEntity d
            join d.devolucion dv
            join dv.reserva r
            where (:estado is null or d.estado = :estado)
              and (:severidad is null or d.severidad = :severidad)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """,
            countQuery = """
            select count(d) from ReporteDanoEntity d
            join d.devolucion dv
            join dv.reserva r
            where (:estado is null or d.estado = :estado)
              and (:severidad is null or d.severidad = :severidad)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """)
    Page<ReporteDanoEntity> buscar(@Param("estado") EstadoIncidencia estado,
                                   @Param("severidad") SeveridadDano severidad,
                                   @Param("clienteId") Long clienteId,
                                   @Param("proveedorId") Long proveedorId,
                                   Pageable pageable);
}
