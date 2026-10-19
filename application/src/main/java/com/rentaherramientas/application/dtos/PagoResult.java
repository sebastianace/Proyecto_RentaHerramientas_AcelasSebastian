package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoPago;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResult(
        Long id,
        BigDecimal monto,
        MetodoPago metodo,
        EstadoPago estado,
        String referenciaTransaccion,
        LocalDateTime fechaPago,
        ReservaResumen reserva,
        FacturaResumen factura) {

    public record ReservaResumen(Long id, String codigo, EstadoReserva estado, Long clienteId, String cliente,
                                 Long proveedorId, String proveedor) {
    }

    /** null si el pago fue rechazado (no genera factura). */
    public record FacturaResumen(Long id, String numero) {
    }
}
