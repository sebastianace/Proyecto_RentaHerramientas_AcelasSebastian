package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaJpaRepository extends JpaRepository<ReservaEntity, Long> {

    /**
     * Unidades comprometidas de una herramienta en un rango de fechas.
     * Dos rangos se cruzan cuando: inicioReserva <= finPedido AND finReserva >= inicioPedido.
     * Usa el índice idx_reservas_estado_fechas (estado, fecha_inicio, fecha_fin).
     */
    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from ReservaDetalleEntity d
            join d.reserva r
            where d.herramienta.id = :herramientaId
              and r.estado in :estados
              and r.fechaInicio <= :fin
              and r.fechaFin >= :inicio
              and (:reservaExcluida is null or r.id <> :reservaExcluida)
            """)
    Long sumarUnidadesComprometidas(@Param("herramientaId") Long herramientaId,
                                    @Param("estados") Collection<EstadoReserva> estados,
                                    @Param("inicio") LocalDate inicio,
                                    @Param("fin") LocalDate fin,
                                    @Param("reservaExcluida") Long reservaExcluida);

    List<ReservaEntity> findByEstadoAndFechaLimitePagoBefore(EstadoReserva estado, LocalDateTime fecha);

    @Query("select coalesce(max(r.id), 0) from ReservaEntity r")
    Long buscarMaximoId();

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "proveedor", "proveedor.usuario"})
    Optional<ReservaEntity> findConRelacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "proveedor", "proveedor.usuario"})
    @Query(value = """
            select r from ReservaEntity r
            where (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
              and (:estado is null or r.estado = :estado)
            """,
            countQuery = """
            select count(r) from ReservaEntity r
            where (:clienteId is null or r.cliente.id = :clienteId)
              and (:proveedorId is null or r.proveedor.id = :proveedorId)
              and (:estado is null or r.estado = :estado)
            """)
    Page<ReservaEntity> buscar(@Param("clienteId") Long clienteId,
                               @Param("proveedorId") Long proveedorId,
                               @Param("estado") EstadoReserva estado,
                               Pageable pageable);

    /** SELECT ... FOR UPDATE sobre la reserva: dos pagos simultáneos de la misma reserva se atienden en fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaEntity r where r.id = :id")
    Optional<ReservaEntity> bloquearPorId(@Param("id") Long id);
}
