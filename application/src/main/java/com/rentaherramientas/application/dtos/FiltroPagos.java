package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoPago;

import java.time.LocalDate;

/** Filtros opcionales del historial de pagos. El caso de uso fuerza clienteId/proveedorId según el rol. */
public record FiltroPagos(EstadoPago estado, Long clienteId, Long proveedorId, LocalDate desde, LocalDate hasta) {
}
