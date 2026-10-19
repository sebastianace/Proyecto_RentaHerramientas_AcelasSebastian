package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ReportesResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.out.ConsultaReportesPort;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Alcance de los reportes según el rol e indicadores derivados (margen, ocupación). */
@ExtendWith(MockitoExtension.class)
class ReportesServiceTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock ConsultaReportesPort consulta;
    @Mock ProveedorRepository proveedorRepository;
    ReportesService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ReportesService(consulta, proveedorRepository, RELOJ);
    }

    @Test
    @DisplayName("Un PROVEEDOR siempre ve sus datos, aunque pida los de otro proveedor")
    void alcanceDelProveedor() {
        Proveedor propio = mock(Proveedor.class);
        when(propio.getId()).thenReturn(1L);
        when(proveedorRepository.buscarPorUsuarioId(3L)).thenReturn(Optional.of(propio));

        servicio.consultarIngresosPorMes(null, null, 5L, new Solicitante(3L, Set.of(RolNombre.PROVEEDOR)));

        // Periodo por defecto: del 1 de enero hasta hoy
        verify(consulta).ingresosPorMes(eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 10, 18)));
    }

    @Test
    @DisplayName("Un CLIENTE no tiene acceso a los reportes")
    void clienteSinAcceso() {
        assertThatThrownBy(() -> servicio.consultarResumen(null, null, null, new Solicitante(23L, Set.of(RolNombre.CLIENTE))))
                .isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(consulta);
    }

    @Test
    @DisplayName("Rentabilidad: utilidad = ingresos − daños; margen y ocupación en porcentaje")
    void rentabilidad() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 10); // 10 días
        when(consulta.rentabilidad(null, desde, hasta)).thenReturn(List.of(new ConsultaReportesPort.DatosRentabilidad(
                1L, "Rotomartillo", "Herramientas del Oriente", 2, new BigDecimal("400000"), new BigDecimal("100000"),
                1, 5)));

        ReportesResult.Rentabilidad r = servicio.consultarRentabilidad(desde, hasta, null,
                new Solicitante(1L, Set.of(RolNombre.ADMIN))).get(0);

        assertThat(r.utilidad()).isEqualByComparingTo("300000");
        assertThat(r.margen()).isEqualByComparingTo("75.00");     // 300.000 / 400.000
        assertThat(r.ocupacion()).isEqualByComparingTo("25.00");  // 5 unidades-día / (2 × 10)
    }

    @Test
    @DisplayName("Si 'desde' es posterior a 'hasta' responde 422")
    void periodoInvertido() {
        assertThatThrownBy(() -> servicio.consultarResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 1, 1), null,
                new Solicitante(1L, Set.of(RolNombre.ADMIN)))).isInstanceOf(ReglaDeNegocioException.class);
    }
}
