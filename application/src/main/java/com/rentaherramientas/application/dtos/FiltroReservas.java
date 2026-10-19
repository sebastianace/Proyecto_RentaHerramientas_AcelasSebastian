package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoReserva;

/** Filtros del listado de reservas. Todos opcionales; el caso de uso los ajusta según el rol. */
public record FiltroReservas(Long clienteId, Long proveedorId, EstadoReserva estado) {
}
