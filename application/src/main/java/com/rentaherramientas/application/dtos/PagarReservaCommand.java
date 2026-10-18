package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.MetodoPago;

/**
 * Datos del pago. Los datos de tarjeta solo viajan hasta la pasarela: NUNCA se guardan en la base de datos.
 *
 * @param numeroTarjeta obligatorio solo para TARJETA_CREDITO y TARJETA_DEBITO
 * @param titular       nombre del titular del medio de pago
 */
public record PagarReservaCommand(MetodoPago metodo, String numeroTarjeta, String titular) {
}
