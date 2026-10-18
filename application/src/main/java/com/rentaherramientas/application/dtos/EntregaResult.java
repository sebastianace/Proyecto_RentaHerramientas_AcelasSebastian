package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.TipoEntrega;

import java.time.LocalDateTime;

public record EntregaResult(
        Long id,
        ReservaReferencia reserva,
        TipoEntrega tipo,
        String direccion,
        LocalDateTime fechaEntrega,
        String registradaPor,
        String observaciones) {
}
