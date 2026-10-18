package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.EstadoPago;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.PagoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PagoJpaRepository extends JpaRepository<PagoEntity, Long> {

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor"})
    Optional<PagoEntity> findConRelacionesById(Long id);

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor"})
    @Query(value = """
            select p from PagoEntity p
            join p.reserva r
            where (:estado is null or p.estado = :estado)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
              and (:desde is null or p.fechaPago >= :desde)
              and (:hasta is null or p.fechaPago < :hasta)
            """,
            countQuery = """
            select count(p) from PagoEntity p
            join p.reserva r
            where (:estado is null or p.estado = :estado)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
              and (:desde is null or p.fechaPago >= :desde)
              and (:hasta is null or p.fechaPago < :hasta)
            """)
    Page<PagoEntity> buscar(@Param("estado") EstadoPago estado,
                            @Param("clienteId") Long clienteId,
                            @Param("proveedorId") Long proveedorId,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            Pageable pageable);
}
