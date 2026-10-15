package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Herramienta;

import java.util.Optional;

/**
 * Puerto de escritura del agregado Herramienta (con sus imágenes).
 * Las consultas del catálogo, que necesitan datos de varias tablas, van por un puerto de consulta aparte.
 */
public interface HerramientaRepository {

    Herramienta guardar(Herramienta herramienta);

    Optional<Herramienta> buscarPorId(Long id);
}
