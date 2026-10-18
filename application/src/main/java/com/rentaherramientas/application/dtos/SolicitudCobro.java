package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.MetodoPago;

import java.math.BigDecimal;

/** Lo que se le envía a la pasarela de pago. */
public record SolicitudCobro(String referenciaInterna, BigDecimal monto, MetodoPago metodo, String numeroTarjeta,
                             String titular) {

    /** Evita que el número de tarjeta aparezca completo si este objeto se imprime en un log. */
    @Override
    public String toString() {
        String tarjeta = numeroTarjeta == null || numeroTarjeta.length() < 4
                ? "-" : "****" + numeroTarjeta.substring(numeroTarjeta.length() - 4);
        return "SolicitudCobro[referencia=" + referenciaInterna + ", monto=" + monto + ", metodo=" + metodo
                + ", tarjeta=" + tarjeta + "]";
    }
}
