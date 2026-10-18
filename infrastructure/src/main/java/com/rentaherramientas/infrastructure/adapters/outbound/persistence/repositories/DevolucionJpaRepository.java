package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.DevolucionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DevolucionJpaRepository extends JpaRepository<DevolucionEntity, Long> {

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor",
            "recibidaPor", "validadaPor"})
    Optional<DevolucionEntity> findConRelacionesById(Long id);

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor",
            "recibidaPor", "validadaPor"})
    Optional<DevolucionEntity> findByReservaId(Long reservaId);

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor",
            "recibidaPor", "validadaPor"})
    @Query(value = """
            select d from DevolucionEntity d
            join d.reserva r
            where (:validada is null or d.validada = :validada)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """,
            countQuery = """
            select count(d) from DevolucionEntity d
            join d.reserva r
            where (:validada is null or d.validada = :validada)
              and (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
            """)
    Page<DevolucionEntity> buscar(@Param("validada") Boolean validada,
                                  @Param("clienteId") Long clienteId,
                                  @Param("proveedorId") Long proveedorId,
                                  Pageable pageable);
}
