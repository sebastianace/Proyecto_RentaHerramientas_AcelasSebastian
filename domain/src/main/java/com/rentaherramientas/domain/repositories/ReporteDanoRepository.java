package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.ReporteDano;

import java.util.Optional;

/** Puerto de escritura de los reportes de daño. */
public interface ReporteDanoRepository {

    ReporteDano guardar(ReporteDano reporte);

    Optional<ReporteDano> buscarPorId(Long id);
}
