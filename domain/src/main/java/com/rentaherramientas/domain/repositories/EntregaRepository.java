package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Entrega;

/** Puerto de escritura de las entregas. Las lecturas van por ConsultaDevolucionesPort. */
public interface EntregaRepository {

    Entrega guardar(Entrega entrega);

    boolean existePorReserva(Long reservaId);
}
