package com.rentaherramientas.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Precio que tendría una reserva, sin guardarla. Sirve para mostrarle el total al cliente antes de confirmar. */
public record CotizacionResult(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int dias,
        List<Linea> lineas,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total) {

    public record Linea(Long herramientaId, String nombre, int cantidad, BigDecimal precioPorDia, BigDecimal subtotal,
                        int unidadesDisponibles) {
    }
}
