package com.rentaherramientas.application.dtos;

/** Resultado de intentar pagar: aprobado o rechazado, con el mensaje de la pasarela y el pago registrado. */
public record PagoRealizadoResult(boolean aprobado, String mensaje, PagoResult pago) {
}
