package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.EntregaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntregaJpaRepository extends JpaRepository<EntregaEntity, Long> {

    boolean existsByReservaId(Long reservaId);

    @EntityGraph(attributePaths = {"reserva", "reserva.cliente", "reserva.cliente.usuario", "reserva.proveedor",
            "registradaPor"})
    Optional<EntregaEntity> findByReservaId(Long reservaId);
}
