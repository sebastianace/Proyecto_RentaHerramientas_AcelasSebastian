package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;

/** Filtros del listado de reportes de daño. Todos opcionales; el caso de uso los ajusta según el rol. */
public record FiltroIncidencias(EstadoIncidencia estado, SeveridadDano severidad, Long clienteId, Long proveedorId) {
}
