package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReservaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.ReservaEntityMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservaRepositoryAdapter implements ReservaRepository {

    private final ReservaJpaRepository jpa;
    private final ClienteJpaRepository clienteJpa;
    private final ProveedorJpaRepository proveedorJpa;
    private final HerramientaJpaRepository herramientaJpa;

    public ReservaRepositoryAdapter(ReservaJpaRepository jpa, ClienteJpaRepository clienteJpa,
                                    ProveedorJpaRepository proveedorJpa, HerramientaJpaRepository herramientaJpa) {
        this.jpa = jpa;
        this.clienteJpa = clienteJpa;
        this.proveedorJpa = proveedorJpa;
        this.herramientaJpa = herramientaJpa;
    }

    @Override
    public Reserva guardar(Reserva reserva) {
        ReservaEntity entidad;
        if (reserva.getId() == null) {
            entidad = new ReservaEntity();
            entidad.setCliente(clienteJpa.getReferenceById(reserva.getClienteId()));
            entidad.setProveedor(proveedorJpa.getReferenceById(reserva.getProveedorId()));
            ReservaEntity destino = entidad;
            reserva.getDetalles().forEach(detalle -> destino.getDetalles().add(ReservaEntityMapper.nuevoDetalle(
                    detalle, destino, herramientaJpa.getReferenceById(detalle.getHerramientaId()))));
        } else {
            entidad = jpa.findById(reserva.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reserva.getId()));
        }
        ReservaEntityMapper.copiarDatos(reserva, entidad);
        return ReservaEntityMapper.aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<Reserva> buscarPorId(Long id) {
        return jpa.findById(id).map(ReservaEntityMapper::aDominio);
    }

    @Override
    public Optional<Reserva> bloquearPorId(Long id) {
        return jpa.bloquearPorId(id).map(ReservaEntityMapper::aDominio);
    }

    @Override
    public int unidadesComprometidas(Long herramientaId, RangoFechas rango, Long reservaExcluida) {
        Long suma = jpa.sumarUnidadesComprometidas(herramientaId, EstadoReserva.OCUPAN_INVENTARIO,
                rango.inicio(), rango.fin(), reservaExcluida);
        return suma == null ? 0 : suma.intValue();
    }

    @Override
    public List<Reserva> buscarAprobadasConPagoVencido(LocalDateTime ahora) {
        return jpa.findByEstadoAndFechaLimitePagoBefore(EstadoReserva.APROBADA, ahora).stream()
                .map(ReservaEntityMapper::aDominio)
                .toList();
    }

    @Override
    public String generarCodigo(int anio) {
        return String.format("RES-%d-%04d", anio, jpa.buscarMaximoId() + 1);
    }
}
