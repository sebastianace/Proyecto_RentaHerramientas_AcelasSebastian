package com.rentaherramientas.domain.model.valueobject;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

    @Test
    @DisplayName("Siempre se guarda con 2 decimales")
    void dosDecimales() {
        assertThat(Dinero.de(35000).monto()).isEqualByComparingTo("35000.00");
        assertThat(Dinero.de(new BigDecimal("10.005")).monto()).isEqualTo(new BigDecimal("10.01"));
    }

    @Test
    @DisplayName("El IVA del 19 % se redondea a 2 decimales (HALF_UP)")
    void iva() {
        assertThat(Dinero.de(105000).porcentaje(19).monto()).isEqualByComparingTo("19950.00");
        assertThat(Dinero.de(new BigDecimal("0.05")).porcentaje(19).monto()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("Sumar y multiplicar no modifican el original (es inmutable)")
    void inmutable() {
        Dinero precio = Dinero.de(1000);
        Dinero total = precio.multiplicar(3).sumar(Dinero.de(500));
        assertThat(total.monto()).isEqualByComparingTo("3500");
        assertThat(precio.monto()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("No se aceptan montos negativos ni nulos")
    void invalidos() {
        assertThatThrownBy(() -> Dinero.de(-1)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Dinero.de(null)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
