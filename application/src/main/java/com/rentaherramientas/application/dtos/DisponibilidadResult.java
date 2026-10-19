package com.rentaherramientas.application.dtos;

import java.time.LocalDate;

/**
 * Disponibilidad de una herramienta para un rango de fechas.
 * unidadesDisponibles = unidadesOperativas − unidadesComprometidas
 */
public record DisponibilidadResult(
        Long herramientaId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int unidadesOperativas,
        int unidadesComprometidas,
        int unidadesDisponibles,
        boolean disponible) {
}
