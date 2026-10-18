package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResult(
        Long id,
        String codigo,
        EstadoReserva estado,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int dias,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        LocalDateTime fechaLimitePago,
        String motivoRechazo,
        String notas,
        LocalDateTime creadaEn,
        ClienteResumen cliente,
        ProveedorResumen proveedor,
        List<Detalle> detalles) {

    public record ClienteResumen(Long id, String nombreCompleto, String email, String telefono) {
    }

    public record ProveedorResumen(Long id, String razonSocial, String ciudad, String telefono) {
    }

    public record Detalle(Long herramientaId, String herramienta, String imagen, int cantidad,
                          BigDecimal precioPorDia, int dias, BigDecimal subtotal) {
    }
}
