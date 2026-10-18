package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.config.PoliticaReservas;
import com.rentaherramientas.application.dtos.CrearReservaCommand;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.out.ConsultaReservasPort;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.DetalleReserva;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Disponibilidad y aprobación de reservas, con los puertos simulados (sin base de datos). */
@ExtendWith(MockitoExtension.class)
class GestionReservasServiceTest {

    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), BOGOTA); // 10:00 en Bogotá
    static final LocalDate HOY = LocalDate.now(RELOJ);

    @Mock ReservaRepository reservaRepository;
    @Mock HerramientaRepository herramientaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock ConsultaReservasPort consultaReservas;

    GestionReservasService servicio;
    final Solicitante cliente = new Solicitante(23L, Set.of(RolNombre.CLIENTE));
    final Solicitante proveedor = new Solicitante(3L, Set.of(RolNombre.PROVEEDOR));

    /** Herramienta 1 del proveedor 1: 6 unidades operativas a $35.000 por día. */
    final Herramienta rotomartillo = Herramienta.reconstruir(1L, 1L, 1L, "Rotomartillo", null, null, null,
            Dinero.de(35000), 6, 0, EstadoHerramienta.ACTIVA, List.of());

    @BeforeEach
    void setUp() {
        servicio = new GestionReservasService(reservaRepository, herramientaRepository, clienteRepository,
                proveedorRepository, consultaReservas, new PoliticaReservas(Duration.ofHours(24), 90), RELOJ);
    }

    private CrearReservaCommand pedido(int cantidad) {
        return new CrearReservaCommand(HOY.plusDays(2), HOY.plusDays(4),
                List.of(new CrearReservaCommand.Item(1L, cantidad)), null);
    }

    private void clienteConId(long id) {
        Cliente c = mock(Cliente.class);
        when(c.getId()).thenReturn(id);
        when(clienteRepository.buscarPorUsuarioId(23L)).thenReturn(Optional.of(c));
    }

    private void proveedorConId(long id) {
        Proveedor p = mock(Proveedor.class);
        when(p.getId()).thenReturn(id);
        when(proveedorRepository.buscarPorUsuarioId(3L)).thenReturn(Optional.of(p));
    }

    private Reserva pendienteDelProveedor(long proveedorId) {
        return Reserva.reconstruir(36L, "RES-2026-0036", 1L, proveedorId,
                new RangoFechas(HOY.plusDays(2), HOY.plusDays(4)), EstadoReserva.PENDIENTE,
                List.of(DetalleReserva.reconstruir(1L, 1L, 2, Dinero.de(35000), 3)), null, null, null);
    }

    @Test
    @DisplayName("Crear: si no hay unidades suficientes en esas fechas, responde 409 y no guarda nada")
    void crearSinDisponibilidad() {
        clienteConId(1L);
        when(herramientaRepository.buscarPorIds(any())).thenReturn(List.of(rotomartillo));
        when(reservaRepository.generarCodigo(2026)).thenReturn("RES-2026-0099");
        when(reservaRepository.unidadesComprometidas(eq(1L), any(), any())).thenReturn(5); // 6 − 5 = 1 libre

        assertThatThrownBy(() -> servicio.crear(pedido(2), cliente))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("hay 1 unidad(es) disponible(s)");
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear: con unidades libres la reserva queda PENDIENTE (todavía no aparta unidades)")
    void crearConDisponibilidad() {
        clienteConId(1L);
        when(herramientaRepository.buscarPorIds(any())).thenReturn(List.of(rotomartillo));
        when(reservaRepository.generarCodigo(2026)).thenReturn("RES-2026-0099");
        when(reservaRepository.unidadesComprometidas(eq(1L), any(), any())).thenReturn(0);
        when(reservaRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(consultaReservas.obtener(any())).thenReturn(Optional.of(mock(ReservaResult.class)));

        servicio.crear(pedido(2), cliente);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(guardada.getValue().getTotal().monto()).isEqualByComparingTo("249900"); // 210.000 + 19 %
    }

    @Test
    @DisplayName("Aprobar: bloquea las herramientas, revalida y fija el plazo de pago en 24 h")
    void aprobar() {
        proveedorConId(1L);
        when(reservaRepository.buscarPorId(36L)).thenReturn(Optional.of(pendienteDelProveedor(1L)));
        when(herramientaRepository.bloquearParaReserva(List.of(1L))).thenReturn(List.of(rotomartillo));
        when(reservaRepository.unidadesComprometidas(eq(1L), any(), eq(36L))).thenReturn(0);
        when(reservaRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(consultaReservas.obtener(36L)).thenReturn(Optional.of(mock(ReservaResult.class)));

        servicio.aprobar(36L, proveedor);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.APROBADA);
        assertThat(guardada.getValue().getFechaLimitePago()).isEqualTo(LocalDateTime.now(RELOJ).plusHours(24));
    }

    @Test
    @DisplayName("Aprobar: si otra aprobación ya tomó las unidades, responde 409")
    void aprobarSinUnidades() {
        proveedorConId(1L);
        when(reservaRepository.buscarPorId(36L)).thenReturn(Optional.of(pendienteDelProveedor(1L)));
        when(herramientaRepository.bloquearParaReserva(List.of(1L))).thenReturn(List.of(rotomartillo));
        when(reservaRepository.unidadesComprometidas(eq(1L), any(), eq(36L))).thenReturn(6);

        assertThatThrownBy(() -> servicio.aprobar(36L, proveedor)).isInstanceOf(ConflictoException.class);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Aprobar: un proveedor no puede aprobar reservas de otro proveedor (403)")
    void aprobarReservaAjena() {
        proveedorConId(2L);
        when(reservaRepository.buscarPorId(36L)).thenReturn(Optional.of(pendienteDelProveedor(1L)));

        assertThatThrownBy(() -> servicio.aprobar(36L, proveedor)).isInstanceOf(AccesoDenegadoException.class);
        verify(herramientaRepository, never()).bloquearParaReserva(any());
    }

    @Test
    @DisplayName("Un cliente no puede ver la reserva de otro cliente aunque conozca su id")
    void verReservaAjena() {
        clienteConId(7L);
        when(reservaRepository.buscarPorId(36L)).thenReturn(Optional.of(pendienteDelProveedor(1L)));

        assertThatThrownBy(() -> servicio.obtener(36L, cliente)).isInstanceOf(AccesoDenegadoException.class);
        verify(consultaReservas, never()).obtener(anyLong());
    }
}
