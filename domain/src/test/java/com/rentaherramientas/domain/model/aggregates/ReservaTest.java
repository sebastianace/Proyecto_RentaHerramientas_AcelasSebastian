package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 18);
    static final LocalDateTime AHORA = HOY.atTime(10, 0);
    static final RangoFechas TRES_DIAS = new RangoFechas(HOY.plusDays(2), HOY.plusDays(4));

    static Herramienta herramienta(long id, long proveedorId, long precio) {
        return Herramienta.reconstruir(id, proveedorId, 1L, "Herramienta " + id, null, null, null, Dinero.de(precio),
                5, 0, EstadoHerramienta.ACTIVA, List.of());
    }

    static Reserva pendiente() {
        return Reserva.solicitar("RES-2026-0001", 1L, TRES_DIAS,
                List.of(new Reserva.LineaSolicitada(herramienta(1, 1, 35000), 2)), null, HOY, 90);
    }

    /** Reserva ya guardada (con id), como la cargaría el repositorio. Contiene la herramienta 1. */
    static Reserva guardada(EstadoReserva estado) {
        return Reserva.reconstruir(36L, "RES-2026-0036", 1L, 1L, TRES_DIAS, estado,
                List.of(DetalleReserva.reconstruir(1L, 1L, 2, Dinero.de(35000), 3)), null, null, null);
    }

    @Nested
    @DisplayName("Al solicitar")
    class Solicitar {

        @Test
        @DisplayName("Calcula subtotal, IVA del 19 % y total, y queda PENDIENTE")
        void calculaTotales() {
            Reserva r = pendiente();
            // 35.000 × 2 unidades × 3 días = 210.000
            assertThat(r.getSubtotal().monto()).isEqualByComparingTo("210000");
            assertThat(r.getIva().monto()).isEqualByComparingTo("39900");
            assertThat(r.getTotal().monto()).isEqualByComparingTo("249900");
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        }

        @Test
        @DisplayName("No mezcla herramientas de dos proveedores")
        void unSoloProveedor() {
            assertThatThrownBy(() -> Reserva.solicitar("RES", 1L, TRES_DIAS, List.of(
                    new Reserva.LineaSolicitada(herramienta(1, 1, 1000), 1),
                    new Reserva.LineaSolicitada(herramienta(2, 2, 1000), 1)), null, HOY, 90))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("mismo proveedor");
        }

        @Test
        @DisplayName("No acepta fechas pasadas ni alquileres de más de 90 días")
        void fechasValidas() {
            RangoFechas pasado = new RangoFechas(HOY.minusDays(1), HOY.plusDays(1));
            RangoFechas largo = new RangoFechas(HOY, HOY.plusDays(90));
            var linea = List.of(new Reserva.LineaSolicitada(herramienta(1, 1, 1000), 1));
            assertThatThrownBy(() -> Reserva.solicitar("RES", 1L, pasado, linea, null, HOY, 90))
                    .isInstanceOf(ReglaDeNegocioException.class);
            assertThatThrownBy(() -> Reserva.solicitar("RES", 1L, largo, linea, null, HOY, 90))
                    .hasMessageContaining("90");
        }
    }

    @Nested
    @DisplayName("Ciclo de estados")
    class Ciclo {

        @Test
        @DisplayName("Camino feliz: PENDIENTE → APROBADA → PAGADA → ENTREGADA → DEVUELTA")
        void caminoFeliz() {
            Reserva r = pendiente();
            r.aprobar(AHORA, Duration.ofHours(24));
            assertThat(r.getFechaLimitePago()).isEqualTo(AHORA.plusHours(24));
            r.confirmarPago(AHORA.plusHours(1));
            r.registrarEntrega();
            r.registrarDevolucion();
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.DEVUELTA);
        }

        @Test
        @DisplayName("No se puede pagar una reserva que no está APROBADA")
        void pagarPendiente() {
            assertThatThrownBy(() -> pendiente().confirmarPago(AHORA))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        @DisplayName("No se puede pagar después del plazo, y el job la puede vencer")
        void plazoVencido() {
            Reserva r = pendiente();
            r.aprobar(AHORA, Duration.ofHours(24));
            LocalDateTime tarde = AHORA.plusHours(25);
            assertThatThrownBy(() -> r.verificarQueSePuedePagar(tarde)).hasMessageContaining("venció");
            assertThat(r.vencerSiCorresponde(tarde)).isTrue();
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.VENCIDA);
        }

        @Test
        @DisplayName("Dentro del plazo no se vence")
        void noVenceAntes() {
            Reserva r = pendiente();
            r.aprobar(AHORA, Duration.ofHours(24));
            assertThat(r.vencerSiCorresponde(AHORA.plusHours(23))).isFalse();
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.APROBADA);
        }

        @Test
        @DisplayName("Una reserva pagada ya no se puede cancelar")
        void cancelarPagada() {
            Reserva r = pendiente();
            r.aprobar(AHORA, Duration.ofHours(24));
            r.confirmarPago(AHORA);
            assertThatThrownBy(r::cancelar).isInstanceOf(ReglaDeNegocioException.class);
        }

        @Test
        @DisplayName("Rechazar exige un motivo")
        void rechazarSinMotivo() {
            assertThatThrownBy(() -> pendiente().rechazar("  ")).isInstanceOf(ReglaDeNegocioException.class);
        }

        @Test
        @DisplayName("No se registra la devolución de un equipo que no se ha entregado")
        void devolverSinEntregar() {
            Reserva r = pendiente();
            r.aprobar(AHORA, Duration.ofHours(24));
            r.confirmarPago(AHORA);
            assertThatThrownBy(r::registrarDevolucion).hasMessageContaining("PAGADA");
        }
    }
}
