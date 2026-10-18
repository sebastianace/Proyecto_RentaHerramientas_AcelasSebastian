package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Factura;

import java.util.Optional;

public interface FacturaRepository {

    Factura guardar(Factura factura);

    Optional<Factura> buscarPorId(Long id);

    /** Siguiente consecutivo, ej. FAC-2026-000028. */
    String generarNumero(int anio);
}
