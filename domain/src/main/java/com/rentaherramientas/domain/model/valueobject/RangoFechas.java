package com.rentaherramientas.domain.model.valueobject;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Periodo de alquiler. Ambos días están incluidos: del lunes al miércoles son 3 días.
 */
public record RangoFechas(LocalDate inicio, LocalDate fin) {

    public RangoFechas {
        if (inicio == null || fin == null) {
            throw new ReglaDeNegocioException("Las fechas de inicio y fin son obligatorias");
        }
        if (fin.isBefore(inicio)) {
            throw new ReglaDeNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    /** Días que se cobran (inclusive). */
    public int dias() {
        return (int) ChronoUnit.DAYS.between(inicio, fin) + 1;
    }

    /**
     * Dos rangos se cruzan si uno empieza antes de que el otro termine y viceversa.
     * Es la misma condición que usa la consulta SQL de disponibilidad.
     */
    public boolean seCruzaCon(RangoFechas otro) {
        return !inicio.isAfter(otro.fin) && !fin.isBefore(otro.inicio);
    }
}
