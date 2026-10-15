package com.rentaherramientas.domain.model.valueobject;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object para montos en pesos colombianos.
 *
 * <p>Usa BigDecimal (nunca double, que tiene errores de redondeo) y siempre 2 decimales.
 * Es inmutable: sumar o multiplicar devuelve un Dinero nuevo.</p>
 */
public record Dinero(BigDecimal monto) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        if (monto == null) {
            throw new ReglaDeNegocioException("El monto es obligatorio");
        }
        if (monto.signum() < 0) {
            throw new ReglaDeNegocioException("El monto no puede ser negativo");
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinero de(BigDecimal monto) {
        return new Dinero(monto);
    }

    public static Dinero de(long monto) {
        return new Dinero(BigDecimal.valueOf(monto));
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(monto.add(otro.monto));
    }

    public Dinero multiplicar(long factor) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(factor)));
    }

    /** Ej. porcentaje(19) calcula el IVA del 19 %. */
    public Dinero porcentaje(int porcentaje) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(porcentaje)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return monto.compareTo(otro.monto);
    }

    @Override
    public String toString() {
        return "$" + monto.toPlainString();
    }
}
