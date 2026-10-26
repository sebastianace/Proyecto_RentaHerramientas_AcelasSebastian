package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Multa por devolver el equipo después de la fecha_fin de la reserva. */
class MultaDevolucionTardiaTest {

    /** Reserva ya guardada: del HOY+2 al HOY+4, con 2 unidades a $35.000 por día = $70.000 de valor diario. */
    final Reserva reserva = ReservaTest.guardada(EstadoReserva.ENTREGADA);
    final LocalDate fin = ReservaTest.TRES_DIAS.fin();

    @Test
    @DisplayName("Devuelta el último día del alquiler no tiene multa")
    void aTiempo() {
        assertThat(reserva.diasDeRetraso(fin)).isZero();
        assertThat(reserva.calcularMulta(fin)).isEqualTo(Dinero.CERO);
    }

    @Test
    @DisplayName("Devuelta antes de tiempo tampoco tiene multa (los días de retraso nunca son negativos)")
    void anticipada() {
        assertThat(reserva.diasDeRetraso(fin.minusDays(2))).isZero();
        assertThat(reserva.calcularMulta(fin.minusDays(2))).isEqualTo(Dinero.CERO);
    }

    @Test
    @DisplayName("Cada día de retraso cuesta el valor diario al 150 %: 3 días x $70.000 x 1,5 = $315.000")
    void tresDiasTarde() {
        assertThat(reserva.diasDeRetraso(fin.plusDays(3))).isEqualTo(3);
        assertThat(reserva.calcularMulta(fin.plusDays(3)).monto()).isEqualByComparingTo("315000");
    }

    @Test
    @DisplayName("La devolución nace con los días de retraso y la multa ya calculados, con la fecha real")
    void devolucionConMulta() {
        LocalDateTime ahora = fin.plusDays(2).atTime(10, 0);

        Devolucion tarde = Devolucion.confirmar(reserva, EstadoEquipo.BUENO, null, 3L, ahora);

        assertThat(tarde.tieneMulta()).isTrue();
        assertThat(tarde.getDiasRetraso()).isEqualTo(2);
        assertThat(tarde.getMulta().monto()).isEqualByComparingTo("210000");
    }

    @Test
    @DisplayName("Una devolución a tiempo queda con multa cero")
    void devolucionSinMulta() {
        Devolucion aTiempo = Devolucion.confirmar(reserva, EstadoEquipo.BUENO, null, 3L, fin.atTime(9, 0));

        assertThat(aTiempo.tieneMulta()).isFalse();
        assertThat(aTiempo.getMulta()).isEqualTo(Dinero.CERO);
    }
}
