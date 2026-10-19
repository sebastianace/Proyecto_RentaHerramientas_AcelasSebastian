package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.PagarReservaCommand;
import com.rentaherramientas.application.dtos.PagoRealizadoResult;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.application.dtos.ResultadoCobro;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.out.ConsultaFacturasPort;
import com.rentaherramientas.application.ports.out.ConsultaPagosPort;
import com.rentaherramientas.application.ports.out.GeneradorFacturaPdfPort;
import com.rentaherramientas.application.ports.out.PasarelaPagoPort;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.DetalleReserva;
import com.rentaherramientas.domain.model.aggregates.Factura;
import com.rentaherramientas.domain.model.aggregates.Pago;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoPago;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.model.valueobject.MetodoPago;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.FacturaRepository;
import com.rentaherramientas.domain.repositories.PagoRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pago en línea y facturación automática, con la pasarela y los repositorios simulados. */
@ExtendWith(MockitoExtension.class)
class GestionPagosServiceTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneId.of("America/Bogota"));
    static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock ReservaRepository reservaRepository;
    @Mock PagoRepository pagoRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock PasarelaPagoPort pasarela;
    @Mock ConsultaPagosPort consultaPagos;
    @Mock ConsultaFacturasPort consultaFacturas;
    @Mock GeneradorFacturaPdfPort generadorPdf;

    GestionPagosService servicio;
    final Solicitante cliente = new Solicitante(23L, Set.of(RolNombre.CLIENTE));
    final PagarReservaCommand tarjeta = new PagarReservaCommand(MetodoPago.TARJETA_CREDITO, "4242424242424242", "JUAN");

    @BeforeEach
    void setUp() {
        servicio = new GestionPagosService(reservaRepository, pagoRepository, facturaRepository, clienteRepository,
                proveedorRepository, pasarela, consultaPagos, consultaFacturas, generadorPdf, RELOJ);
        Cliente c = mock(Cliente.class);
        when(c.getId()).thenReturn(1L);
        when(clienteRepository.buscarPorUsuarioId(23L)).thenReturn(Optional.of(c));
    }

    private Reserva reserva(EstadoReserva estado, long clienteId) {
        LocalDate inicio = LocalDate.of(2026, 11, 10);
        return Reserva.reconstruir(36L, "RES-2026-0036", clienteId, 1L, new RangoFechas(inicio, inicio.plusDays(2)),
                estado, List.of(DetalleReserva.reconstruir(1L, 1L, 1, Dinero.de(35000), 3)),
                AHORA.plusHours(20), null, null);
    }

    private void pagoGuardadoConId() {
        when(pagoRepository.guardar(any())).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            return Pago.reconstruir(31L, p.getReservaId(), p.getMonto(), p.getMetodo(), p.getEstado(),
                    p.getReferenciaTransaccion(), p.getFechaPago());
        });
        when(consultaPagos.obtener(31L)).thenReturn(Optional.of(mock(PagoResult.class)));
    }

    @Test
    @DisplayName("Pago aprobado: la reserva pasa a PAGADA y se emite la factura con el total de la reserva")
    void pagoAprobado() {
        when(reservaRepository.bloquearPorId(36L)).thenReturn(Optional.of(reserva(EstadoReserva.APROBADA, 1L)));
        when(pasarela.cobrar(any())).thenReturn(new ResultadoCobro(true, "TX-OK", "Pago aprobado"));
        pagoGuardadoConId();
        when(facturaRepository.generarNumero(2026)).thenReturn("FAC-2026-000028");

        PagoRealizadoResult resultado = servicio.pagar(36L, tarjeta, cliente);

        assertThat(resultado.aprobado()).isTrue();
        ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).guardar(reserva.capture());
        assertThat(reserva.getValue().getEstado()).isEqualTo(EstadoReserva.PAGADA);
        ArgumentCaptor<Factura> factura = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).guardar(factura.capture());
        assertThat(factura.getValue().getNumero()).isEqualTo("FAC-2026-000028");
        assertThat(factura.getValue().getTotal().monto()).isEqualByComparingTo("124950"); // 105.000 + 19 %
    }

    @Test
    @DisplayName("Pago rechazado: queda registrado como RECHAZADO, no hay factura y la reserva sigue APROBADA")
    void pagoRechazado() {
        when(reservaRepository.bloquearPorId(36L)).thenReturn(Optional.of(reserva(EstadoReserva.APROBADA, 1L)));
        when(pasarela.cobrar(any())).thenReturn(new ResultadoCobro(false, "TX-NO", "Fondos insuficientes"));
        pagoGuardadoConId();

        PagoRealizadoResult resultado = servicio.pagar(36L, tarjeta, cliente);

        assertThat(resultado.aprobado()).isFalse();
        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).guardar(pago.capture());
        assertThat(pago.getValue().getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        verify(reservaRepository, never()).guardar(any());
        verify(facturaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Nunca se cobra una reserva que no se puede confirmar (por ejemplo, PENDIENTE)")
    void noCobraSiNoEstaAprobada() {
        when(reservaRepository.bloquearPorId(36L)).thenReturn(Optional.of(reserva(EstadoReserva.PENDIENTE, 1L)));

        assertThatThrownBy(() -> servicio.pagar(36L, tarjeta, cliente)).isInstanceOf(ReglaDeNegocioException.class);
        verifyNoInteractions(pasarela, pagoRepository, facturaRepository);
    }

    @Test
    @DisplayName("Un cliente no puede pagar la reserva de otro cliente")
    void reservaAjena() {
        when(reservaRepository.bloquearPorId(36L)).thenReturn(Optional.of(reserva(EstadoReserva.APROBADA, 7L)));

        assertThatThrownBy(() -> servicio.pagar(36L, tarjeta, cliente)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(pasarela);
    }

    @Test
    @DisplayName("Con tarjeta, el número debe tener entre 13 y 19 dígitos")
    void tarjetaInvalida() {
        when(reservaRepository.bloquearPorId(36L)).thenReturn(Optional.of(reserva(EstadoReserva.APROBADA, 1L)));
        PagarReservaCommand mala = new PagarReservaCommand(MetodoPago.TARJETA_DEBITO, "1234", "JUAN");

        assertThatThrownBy(() -> servicio.pagar(36L, mala, cliente)).hasMessageContaining("13 a 19");
        verifyNoInteractions(pasarela);
    }
}
