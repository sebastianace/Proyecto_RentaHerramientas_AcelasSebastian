package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.aggregates.Entrega;
import com.rentaherramientas.domain.repositories.EntregaRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.EntregaEntity;
import org.springframework.stereotype.Repository;

/** Las entregas son registros históricos: se crean y no se modifican. */
@Repository
public class EntregaRepositoryAdapter implements EntregaRepository {

    private final EntregaJpaRepository jpa;
    private final ReservaJpaRepository reservaJpa;
    private final UsuarioJpaRepository usuarioJpa;

    public EntregaRepositoryAdapter(EntregaJpaRepository jpa, ReservaJpaRepository reservaJpa,
                                    UsuarioJpaRepository usuarioJpa) {
        this.jpa = jpa;
        this.reservaJpa = reservaJpa;
        this.usuarioJpa = usuarioJpa;
    }

    @Override
    public Entrega guardar(Entrega entrega) {
        if (entrega.getId() != null) {
            throw new IllegalStateException("Una entrega registrada no se puede modificar");
        }
        EntregaEntity entidad = new EntregaEntity();
        entidad.setReserva(reservaJpa.getReferenceById(entrega.getReservaId()));
        entidad.setTipo(entrega.getTipo());
        entidad.setDireccionEntrega(entrega.getDireccion());
        entidad.setFechaEntrega(entrega.getFecha());
        entidad.setRegistradaPor(usuarioJpa.getReferenceById(entrega.getRegistradaPor()));
        entidad.setObservaciones(entrega.getObservaciones());
        EntregaEntity guardada = jpa.saveAndFlush(entidad);
        return Entrega.reconstruir(guardada.getId(), entrega.getReservaId(), guardada.getTipo(),
                guardada.getDireccionEntrega(), guardada.getFechaEntrega(), entrega.getRegistradaPor(),
                guardada.getObservaciones());
    }

    @Override
    public boolean existePorReserva(Long reservaId) {
        return jpa.existsByReservaId(reservaId);
    }
}
