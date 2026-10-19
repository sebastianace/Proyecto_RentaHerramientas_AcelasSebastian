package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;
import com.rentaherramientas.domain.model.valueobject.TipoEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de entrega, devolución, daños e inventario (fase 6). */
class DevolucionesYDanosTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 18, 10, 0);

    @Test
    @DisplayName("Una entrega a domicilio exige dirección; en sede la dirección se descarta")
    void direccionDeEntrega() {
        Reserva r = ReservaTest.guardada(EstadoReserva.ENTREGADA);
        assertThatThrownBy(() -> Entrega.registrar(r, TipoEntrega.DOMICILIO, " ", null, 3L, AHORA))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("dirección");
        Entrega enSede = Entrega.registrar(r, TipoEntrega.RECOGIDA_EN_SEDE, "Calle 1", null, 3L, AHORA);
        assertThat(enSede.getDireccion()).isNull();
    }

    @Test
    @DisplayName("Una devolución solo se valida una vez")
    void validarUnaVez() {
        Devolucion d = Devolucion.reconstruir(1L, 1L, AHORA, EstadoEquipo.BUENO, null, 3L, false, null, null);
        d.validar(1L, AHORA);
        assertThat(d.isValidada()).isTrue();
        assertThatThrownBy(() -> d.validar(1L, AHORA)).hasMessageContaining("ya fue validada");
    }

    @Test
    @DisplayName("No se reportan daños de un equipo devuelto en BUEN estado ni de herramientas ajenas a la reserva")
    void reglasDelReporte() {
        Reserva r = ReservaTest.guardada(EstadoReserva.DEVUELTA); // contiene la herramienta 1
        Devolucion buena = Devolucion.reconstruir(1L, 1L, AHORA, EstadoEquipo.BUENO, null, 3L, false, null, null);
        Devolucion danada = Devolucion.reconstruir(2L, 1L, AHORA, EstadoEquipo.CON_DANOS, null, 3L, false, null, null);
        assertThatThrownBy(() -> ReporteDano.reportar(buena, r, 1L, "x", SeveridadDano.LEVE, Dinero.CERO, 3L))
                .hasMessageContaining("buen estado");
        assertThatThrownBy(() -> ReporteDano.reportar(danada, r, 99L, "x", SeveridadDano.LEVE, Dinero.CERO, 3L))
                .hasMessageContaining("no hace parte");
        assertThat(ReporteDano.reportar(danada, r, 1L, "Mandril roto", SeveridadDano.GRAVE, Dinero.de(80000), 3L)
                .sacaDeServicio()).isTrue();
    }

    @Test
    @DisplayName("Las incidencias avanzan un paso a la vez: ABIERTO → EN_REVISION → RESUELTO")
    void cicloIncidencia() {
        ReporteDano d = ReporteDano.reconstruir(1L, 1L, 1L, "x", SeveridadDano.MODERADO, Dinero.CERO,
                EstadoIncidencia.ABIERTO, 3L);
        assertThatThrownBy(() -> d.avanzarA(EstadoIncidencia.RESUELTO)).isInstanceOf(ReglaDeNegocioException.class);
        d.avanzarA(EstadoIncidencia.EN_REVISION);
        d.avanzarA(EstadoIncidencia.RESUELTO);
        assertThat(d.estaResuelto()).isTrue();
        assertThatThrownBy(() -> d.avanzarA(EstadoIncidencia.ABIERTO)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Sacar y reintegrar unidades nunca deja más fuera de servicio que el stock")
    void unidadesFueraDeServicio() {
        Herramienta h = ReservaTest.herramienta(1, 1, 1000); // stock 5, 0 fuera de servicio
        h.sacarDeServicio(5);
        assertThat(h.unidadesOperativas()).isZero();
        assertThat(h.admiteReservas()).isFalse();
        assertThatThrownBy(() -> h.sacarDeServicio(1)).hasMessageContaining("operativa");
        h.reintegrarAServicio(2);
        assertThat(h.unidadesOperativas()).isEqualTo(2);
        assertThatThrownBy(() -> h.reintegrarAServicio(4)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
