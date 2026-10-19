package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoReserva;

/** Resumen de la reserva a la que pertenece una entrega, devolución o daño (para no repetir campos). */
public record ReservaReferencia(Long id, String codigo, EstadoReserva estado, Long clienteId, String cliente,
                                Long proveedorId, String proveedor) {
}
