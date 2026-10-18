package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.FacturaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FacturaJpaRepository extends JpaRepository<FacturaEntity, Long> {

    @EntityGraph(attributePaths = {"pago", "pago.reserva", "pago.reserva.cliente", "pago.reserva.cliente.usuario",
            "pago.reserva.proveedor", "pago.reserva.proveedor.usuario"})
    Optional<FacturaEntity> findConRelacionesById(Long id);

    @EntityGraph(attributePaths = {"pago", "pago.reserva", "pago.reserva.cliente", "pago.reserva.cliente.usuario",
            "pago.reserva.proveedor", "pago.reserva.proveedor.usuario"})
    @Query(value = """
            select f from FacturaEntity f
            join f.pago p
            join p.reserva r
            where (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """,
            countQuery = """
            select count(f) from FacturaEntity f
            join f.pago p
            join p.reserva r
            where (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """)
    Page<FacturaEntity> buscar(@Param("clienteId") Long clienteId,
                               @Param("proveedorId") Long proveedorId,
                               Pageable pageable);

    @Query("select coalesce(max(f.id), 0) from FacturaEntity f")
    Long buscarMaximoId();
}
