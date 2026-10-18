package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Devolucion;

import java.util.Optional;

/** Puerto de escritura de las devoluciones. */
public interface DevolucionRepository {

    Devolucion guardar(Devolucion devolucion);

    Optional<Devolucion> buscarPorId(Long id);
}
