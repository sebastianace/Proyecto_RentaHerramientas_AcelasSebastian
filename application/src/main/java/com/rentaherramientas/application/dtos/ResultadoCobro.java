package com.rentaherramientas.application.dtos;

/** Respuesta de la pasarela: si aprobó, su referencia de transacción y un mensaje para el cliente. */
public record ResultadoCobro(boolean aprobado, String referenciaTransaccion, String mensaje) {
}
