package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoPago;
import com.rentaherramientas.domain.model.valueobject.MetodoPago;

import java.time.LocalDateTime;

/**
 * Intento de pago de una reserva. Se guarda TODO intento, aprobado o rechazado:
 * así el historial de pagos muestra también los rechazos (útil para soporte y auditoría).
 *
 * <p>Nunca guarda datos de la tarjeta: solo la referencia que devuelve la pasarela.</p>
 */
public class Pago {

    private final Long id;
    private final Long reservaId;
    private final Dinero monto;
    private final MetodoPago metodo;
    private final EstadoPago estado;
    private final String referenciaTransaccion;
    private final LocalDateTime fechaPago;

    private Pago(Long id, Long reservaId, Dinero monto, MetodoPago metodo, EstadoPago estado,
                 String referenciaTransaccion, LocalDateTime fechaPago) {
        if (reservaId == null || metodo == null || estado == null || fechaPago == null) {
            throw new ReglaDeNegocioException("El pago requiere reserva, método, estado y fecha");
        }
        if (monto == null || !monto.esPositivo()) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor que cero");
        }
        this.id = id;
        this.reservaId = reservaId;
        this.monto = monto;
        this.metodo = metodo;
        this.estado = estado;
        this.referenciaTransaccion = ReglaDeNegocioException.requerido(referenciaTransaccion, "referenciaTransaccion");
        this.fechaPago = fechaPago;
    }

    /** Registra el resultado que devolvió la pasarela para una reserva. El monto es siempre el total de la reserva. */
    public static Pago registrar(Reserva reserva, MetodoPago metodo, boolean aprobado, String referenciaTransaccion,
                                 LocalDateTime fecha) {
        return new Pago(null, reserva.getId(), reserva.getTotal(), metodo,
                aprobado ? EstadoPago.APROBADO : EstadoPago.RECHAZADO, referenciaTransaccion, fecha);
    }

    public static Pago reconstruir(Long id, Long reservaId, Dinero monto, MetodoPago metodo, EstadoPago estado,
                                   String referenciaTransaccion, LocalDateTime fechaPago) {
        return new Pago(id, reservaId, monto, metodo, estado, referenciaTransaccion, fechaPago);
    }

    public boolean estaAprobado() {
        return estado == EstadoPago.APROBADO;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Dinero getMonto() { return monto; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public String getReferenciaTransaccion() { return referenciaTransaccion; }
    public LocalDateTime getFechaPago() { return fechaPago; }
}
