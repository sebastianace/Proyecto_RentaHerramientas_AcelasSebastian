package com.rentaherramientas.application.dtos;

/** Filtros del listado de devoluciones. Todos opcionales; el caso de uso los ajusta según el rol. */
public record FiltroDevoluciones(Boolean validada, Long clienteId, Long proveedorId) {
}
