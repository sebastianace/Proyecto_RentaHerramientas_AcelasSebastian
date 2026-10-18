package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Devolucion;
import com.rentaherramientas.domain.repositories.DevolucionRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.DevolucionEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.DevolucionesEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Al actualizar carga la entidad existente y le copia los cambios (no la reconstruye desde cero). */
@Repository
public class DevolucionRepositoryAdapter implements DevolucionRepository {

    private final DevolucionJpaRepository jpa;
    private final ReservaJpaRepository reservaJpa;
    private final UsuarioJpaRepository usuarioJpa;

    public DevolucionRepositoryAdapter(DevolucionJpaRepository jpa, ReservaJpaRepository reservaJpa,
                                       UsuarioJpaRepository usuarioJpa) {
        this.jpa = jpa;
        this.reservaJpa = reservaJpa;
        this.usuarioJpa = usuarioJpa;
    }

    @Override
    public Devolucion guardar(Devolucion devolucion) {
        DevolucionEntity entidad;
        if (devolucion.getId() == null) {
            entidad = new DevolucionEntity();
            entidad.setReserva(reservaJpa.getReferenceById(devolucion.getReservaId()));
            entidad.setFechaDevolucion(devolucion.getFecha());
            entidad.setEstadoEquipo(devolucion.getEstadoEquipo());
            entidad.setObservaciones(devolucion.getObservaciones());
            entidad.setRecibidaPor(usuarioJpa.getReferenceById(devolucion.getRecibidaPor()));
        } else {
            entidad = jpa.findById(devolucion.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Devolución", devolucion.getId()));
        }
        // Lo único que cambia después de creada es la validación del ADMIN
        entidad.setValidada(devolucion.isValidada());
        entidad.setValidadaPor(devolucion.getValidadaPor() == null ? null
                : usuarioJpa.getReferenceById(devolucion.getValidadaPor()));
        entidad.setFechaValidacion(devolucion.getFechaValidacion());
        return DevolucionesEntityMapper.aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<Devolucion> buscarPorId(Long id) {
        return jpa.findConRelacionesById(id).map(DevolucionesEntityMapper::aDominio);
    }
}
