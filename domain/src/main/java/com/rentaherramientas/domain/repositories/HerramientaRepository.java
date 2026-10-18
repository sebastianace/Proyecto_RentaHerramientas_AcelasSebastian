package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Herramienta;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de escritura del agregado Herramienta (con sus imágenes).
 * Las consultas del catálogo, que necesitan datos de varias tablas, van por un puerto de consulta aparte.
 */
public interface HerramientaRepository {

    Herramienta guardar(Herramienta herramienta);

    Optional<Herramienta> buscarPorId(Long id);

    List<Herramienta> buscarPorIds(Collection<Long> ids);

    /**
     * Carga las herramientas BLOQUEANDO sus filas hasta que termine la transacción (SELECT ... FOR UPDATE).
     * Evita que dos aprobaciones simultáneas entreguen la última unidad a dos clientes distintos.
     */
    List<Herramienta> bloquearParaReserva(Collection<Long> ids);
}
