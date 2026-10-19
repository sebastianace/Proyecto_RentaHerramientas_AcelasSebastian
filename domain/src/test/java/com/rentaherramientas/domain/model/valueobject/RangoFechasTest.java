package com.rentaherramientas.domain.model.valueobject;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangoFechasTest {

    private static final LocalDate D1 = LocalDate.of(2026, 11, 10);

    @Test
    @DisplayName("Los días de alquiler se cuentan de forma inclusiva: del 10 al 12 son 3 días")
    void diasInclusivos() {
        assertThat(new RangoFechas(D1, D1.plusDays(2)).dias()).isEqualTo(3);
        assertThat(new RangoFechas(D1, D1).dias()).isEqualTo(1);
    }

    @Test
    @DisplayName("La fecha de fin no puede ser anterior a la de inicio")
    void finAntesDeInicio() {
        assertThatThrownBy(() -> new RangoFechas(D1, D1.minusDays(1)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    @DisplayName("Dos rangos se cruzan aunque solo compartan un día")
    void cruceEnElBorde() {
        RangoFechas a = new RangoFechas(D1, D1.plusDays(2));
        assertThat(a.seCruzaCon(new RangoFechas(D1.plusDays(2), D1.plusDays(5)))).isTrue();
        assertThat(a.seCruzaCon(new RangoFechas(D1.minusDays(3), D1))).isTrue();
    }

    @Test
    @DisplayName("Rangos consecutivos sin días en común no se cruzan")
    void sinCruce() {
        RangoFechas a = new RangoFechas(D1, D1.plusDays(2));
        assertThat(a.seCruzaCon(new RangoFechas(D1.plusDays(3), D1.plusDays(4)))).isFalse();
    }
}
