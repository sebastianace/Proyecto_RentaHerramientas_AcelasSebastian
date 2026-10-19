package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ConfirmarDevolucionCommand;
import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.ReportarDanoCommand;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.out.ConsultaDevolucionesPort;
import com.rentaherramientas.domain.model.aggregates.DetalleReserva;
import com.rentaherramientas.domain.model.aggregates.Devolucion;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.ReporteDano;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.DevolucionRepository;
import com.rentaherramientas.domain.repositories.EntregaRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReporteDanoRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Devoluciones y la regla del daño GRAVE (saca de servicio una unidad). */
@ExtendWith(MockitoExtension.class)
class GestionDevolucionesServiceTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock ReservaRepository reservaRepository;
    @Mock EntregaRepository entregaRepository;
    @Mock DevolucionRepository devolucionRepository;
    @Mock ReporteDanoRepository reporteDanoRepository;
    @Mock HerramientaRepository herramientaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock ConsultaDevolucionesPort consulta;

    GestionDevolucionesService servicio;
    final Solicitante proveedor = new Solicitante(3L, Set.of(RolNombre.PROVEEDOR));
    final Solicitante admin = new Solicitante(1L, Set.of(RolNombre.ADMIN));

    @BeforeEach
    void setUp() {
        servicio = new GestionDevolucionesService(reservaRepository, entregaRepository, devolucionRepository,
                reporteDanoRepository, herramientaRepository, clienteRepository, proveedorRepository, consulta, RELOJ);
    }

    private Reserva entregada() {
        LocalDate inicio = LocalDate.of(2026, 10, 10);
        return Reserva.reconstruir(21L, "RES-2026-0021", 1L, 1L, new RangoFechas(inicio, inicio.plusDays(4)),
                EstadoReserva.ENTREGADA, List.of(DetalleReserva.reconstruir(1L, 1L, 1, Dinero.de(35000), 5)),
                null, null, null);
    }

    private Herramienta herramienta(int fueraServicio) {
        return Herramienta.reconstruir(1L, 1L, 1L, "Rotomartillo", null, null, null, Dinero.de(35000), 6,
                fueraServicio, EstadoHerramienta.ACTIVA, List.of());
    }

    @Test
    @DisplayName("Devolución con un daño GRAVE: la reserva queda DEVUELTA y la herramienta pierde una unidad operativa")
    void danoGraveSacaDeServicio() {
        Proveedor p = mock(Proveedor.class);
        when(p.getId()).thenReturn(1L);
        when(proveedorRepository.buscarPorUsuarioId(3L)).thenReturn(Optional.of(p));
        when(reservaRepository.bloquearPorId(21L)).thenReturn(Optional.of(entregada()));
        when(devolucionRepository.guardar(any())).thenAnswer(i -> {
            Devolucion d = i.getArgument(0);
            return Devolucion.reconstruir(50L, d.getReservaId(), d.getFecha(), d.getEstadoEquipo(), null,
                    d.getRecibidaPor(), false, null, null);
        });
        when(reporteDanoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(herramientaRepository.bloquearParaReserva(List.of(1L))).thenReturn(List.of(herramienta(0)));
        when(consulta.obtenerDevolucion(50L)).thenReturn(Optional.of(mock(DevolucionResult.class)));

        servicio.confirmarDevolucion(21L, new ConfirmarDevolucionCommand(EstadoEquipo.CON_DANOS, null, List.of(
                new ReportarDanoCommand(1L, "Carcasa partida", SeveridadDano.GRAVE, new BigDecimal("180000")))), proveedor);

        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).guardar(reserva.capture());
        assertThat(reserva.getValue().getEstado()).isEqualTo(EstadoReserva.DEVUELTA);
        ArgumentCaptor<Herramienta> h = ArgumentCaptor.forClass(Herramienta.class);
        verify(herramientaRepository).guardar(h.capture());
        assertThat(h.getValue().getUnidadesFueraServicio()).isEqualTo(1);
        assertThat(h.getValue().unidadesOperativas()).isEqualTo(5);
    }

    @Test
    @DisplayName("Resolver un daño GRAVE devuelve la unidad reparada al inventario")
    void resolverDanoGraveReintegra() {
        ReporteDano enRevision = ReporteDano.reconstruir(9L, 50L, 1L, "Carcasa partida", SeveridadDano.GRAVE,
                Dinero.de(180000), EstadoIncidencia.EN_REVISION, 3L);
        when(reporteDanoRepository.buscarPorId(9L)).thenReturn(Optional.of(enRevision));
        when(herramientaRepository.bloquearParaReserva(List.of(1L))).thenReturn(List.of(herramienta(1)));
        when(consulta.obtenerReporte(9L)).thenReturn(Optional.of(mock(ReporteDanoResult.class)));

        servicio.cambiarEstadoIncidencia(9L, EstadoIncidencia.RESUELTO, admin);

        ArgumentCaptor<Herramienta> h = ArgumentCaptor.forClass(Herramienta.class);
        verify(herramientaRepository).guardar(h.capture());
        assertThat(h.getValue().getUnidadesFueraServicio()).isZero();
    }

    @Test
    @DisplayName("Solo el ADMIN valida devoluciones, aunque la petición llegue por otro camino")
    void validarSoloAdmin() {
        assertThatThrownBy(() -> servicio.validarDevolucion(50L, proveedor)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(devolucionRepository);
    }

    @Test
    @DisplayName("Un proveedor no registra la entrega de una reserva de otro proveedor")
    void entregaAjena() {
        Proveedor otro = mock(Proveedor.class);
        when(otro.getId()).thenReturn(2L);
        when(proveedorRepository.buscarPorUsuarioId(3L)).thenReturn(Optional.of(otro));
        when(reservaRepository.bloquearPorId(21L)).thenReturn(Optional.of(entregada()));

        assertThatThrownBy(() -> servicio.registrarEntrega(21L, null, proveedor))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(entregaRepository, never()).guardar(any());
    }
}
