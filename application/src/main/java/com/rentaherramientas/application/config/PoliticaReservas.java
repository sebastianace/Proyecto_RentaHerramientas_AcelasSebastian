package com.rentaherramientas.application.config;

import java.time.Duration;

/**
 * Parámetros de negocio de las reservas. Se definen en application.yml (app.reservas.*)
 * y llegan aquí como un objeto Java puro: la aplicación no lee archivos de configuración.
 *
 * @param plazoPago           tiempo que tiene el cliente para pagar una reserva aprobada
 * @param diasMaximosAlquiler duración máxima de un alquiler
 */
public record PoliticaReservas(Duration plazoPago, int diasMaximosAlquiler) {

    public PoliticaReservas {
        if (plazoPago == null || plazoPago.isNegative() || plazoPago.isZero()) {
            throw new IllegalArgumentException("El plazo de pago debe ser positivo");
        }
        if (diasMaximosAlquiler < 1) {
            throw new IllegalArgumentException("Los días máximos de alquiler deben ser al menos 1");
        }
    }
}
