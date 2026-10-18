package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;

/**
 * Línea de una reserva: una herramienta, cuántas unidades y a qué precio.
 * El precio queda CONGELADO al crear la reserva: si el proveedor lo cambia después, esta línea no cambia.
 */
public class DetalleReserva {

    private final Long id;
    private final Long herramientaId;
    private final int cantidad;
    private final Dinero precioPorDia;
    private final int dias;
    private final Dinero subtotal;

    private DetalleReserva(Long id, Long herramientaId, int cantidad, Dinero precioPorDia, int dias) {
        if (herramientaId == null) {
            throw new ReglaDeNegocioException("La herramienta es obligatoria");
        }
        if (cantidad < 1) {
            throw new ReglaDeNegocioException("La cantidad debe ser al menos 1");
        }
        this.id = id;
        this.herramientaId = herramientaId;
        this.cantidad = cantidad;
        this.precioPorDia = precioPorDia;
        this.dias = dias;
        this.subtotal = precioPorDia.multiplicar((long) cantidad * dias);
    }

    static DetalleReserva nuevo(Long herramientaId, int cantidad, Dinero precioPorDia, int dias) {
        return new DetalleReserva(null, herramientaId, cantidad, precioPorDia, dias);
    }

    public static DetalleReserva reconstruir(Long id, Long herramientaId, int cantidad, Dinero precioPorDia, int dias) {
        return new DetalleReserva(id, herramientaId, cantidad, precioPorDia, dias);
    }

    public Long getId() { return id; }
    public Long getHerramientaId() { return herramientaId; }
    public int getCantidad() { return cantidad; }
    public Dinero getPrecioPorDia() { return precioPorDia; }
    public int getDias() { return dias; }
    public Dinero getSubtotal() { return subtotal; }
}
