package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoFactura;
import com.rentaherramientas.domain.model.valueobject.MetodoPago;
import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Factura completa: todo lo necesario para mostrarla o generar su PDF. */
public record FacturaResult(
        Long id,
        String numero,
        EstadoFactura estado,
        LocalDateTime fechaEmision,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        Emisor emisor,
        Cliente cliente,
        Reserva reserva,
        Pago pago,
        List<Linea> lineas) {

    public record Emisor(Long proveedorId, String razonSocial, String nit, String direccion, String ciudad,
                         String email, String telefono) {
    }

    public record Cliente(Long clienteId, String nombreCompleto, TipoDocumento tipoDocumento, String numeroDocumento,
                          String direccion, String ciudad, String email) {
    }

    public record Reserva(Long id, String codigo, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    public record Pago(Long id, MetodoPago metodo, String referenciaTransaccion, LocalDateTime fechaPago) {
    }

    public record Linea(String herramienta, int cantidad, int dias, BigDecimal precioPorDia, BigDecimal subtotal) {
    }
}
