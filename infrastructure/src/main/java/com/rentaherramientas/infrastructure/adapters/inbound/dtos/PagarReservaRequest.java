package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.PagarReservaCommand;
import com.rentaherramientas.domain.model.valueobject.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = """
        Pago en línea (pasarela SIMULADA). Tarjetas de prueba:
        terminada en 0000 = rechazada por fondos; terminada en 1111 = rechazada por el banco; cualquier otra = aprobada.
        PSE y NEQUI siempre se aprueban. Los datos de la tarjeta nunca se guardan.""")
public record PagarReservaRequest(
        @Schema(example = "TARJETA_CREDITO")
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodo,

        @Schema(example = "4242424242424242", description = "Obligatorio solo para tarjetas")
        @Pattern(regexp = "^[0-9 ]{13,23}$", message = "El número de tarjeta debe tener entre 13 y 19 dígitos")
        String numeroTarjeta,

        @Schema(example = "JUAN PABLO ARDILA")
        @NotBlank(message = "El titular es obligatorio")
        @Size(max = 100)
        String titular) {

    public PagarReservaCommand aCommand() {
        return new PagarReservaCommand(metodo, numeroTarjeta, titular);
    }

    /** Nunca imprimir el número completo de la tarjeta (por ejemplo, en logs de depuración). */
    @Override
    public String toString() {
        return "PagarReservaRequest[metodo=" + metodo + ", titular=" + titular + "]";
    }
}
