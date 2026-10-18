package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ReportesResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.in.ReportesUseCase;
import com.rentaherramientas.application.ports.out.ConsultaReportesPort;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ProveedorRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Panel de reportes. Hace tres cosas que NO son SQL y por eso viven en la capa de aplicación:
 * <ol>
 *   <li><b>Alcance según el rol:</b> un PROVEEDOR siempre ve solo lo suyo, aunque mande otro proveedorId.</li>
 *   <li><b>Periodo:</b> valida las fechas y pone un periodo por defecto usando el {@link Clock}.</li>
 *   <li><b>Indicadores derivados:</b> ticket promedio, tasa de aprobación, margen, ocupación y alertas.</li>
 * </ol>
 * Las sumas y conteos los hace la base de datos a través de {@link ConsultaReportesPort}.
 */
public class ReportesService implements ReportesUseCase {

    private static final int LIMITE_PANEL = 5;
    private static final int LIMITE_MAXIMO = 50;
    private static final long DIAS_MAXIMOS_PERIODO = 731;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ConsultaReportesPort consulta;
    private final ProveedorRepository proveedorRepository;
    private final Clock reloj;

    public ReportesService(ConsultaReportesPort consulta, ProveedorRepository proveedorRepository, Clock reloj) {
        this.consulta = consulta;
        this.proveedorRepository = proveedorRepository;
        this.reloj = reloj;
    }

    @Override
    public ReportesResult.Panel consultarPanel(LocalDate desde, LocalDate hasta, Long proveedorId,
                                               Solicitante solicitante) {
        Long alcance = alcance(proveedorId, solicitante);
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        return new ReportesResult.Panel(
                resumen(alcance, periodo),
                consulta.ingresosPorMes(alcance, periodo.desde(), periodo.hasta()),
                consulta.herramientasMasAlquiladas(alcance, periodo.desde(), periodo.hasta(), LIMITE_PANEL),
                consulta.clientesFrecuentes(alcance, periodo.desde(), periodo.hasta(), LIMITE_PANEL));
    }

    @Override
    public ReportesResult.Resumen consultarResumen(LocalDate desde, LocalDate hasta, Long proveedorId,
                                                   Solicitante solicitante) {
        return resumen(alcance(proveedorId, solicitante), periodo(desde, hasta));
    }

    @Override
    public List<ReportesResult.IngresoMensual> consultarIngresosPorMes(LocalDate desde, LocalDate hasta,
                                                                       Long proveedorId, Solicitante solicitante) {
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        return consulta.ingresosPorMes(alcance(proveedorId, solicitante), periodo.desde(), periodo.hasta());
    }

    @Override
    public List<ReportesResult.IngresoProveedor> consultarIngresosPorProveedor(LocalDate desde, LocalDate hasta,
                                                                               Solicitante solicitante) {
        if (!solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo el administrador puede comparar proveedores");
        }
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        return consulta.ingresosPorProveedor(periodo.desde(), periodo.hasta());
    }

    @Override
    public List<ReportesResult.HerramientaAlquilada> consultarMasAlquiladas(LocalDate desde, LocalDate hasta,
                                                                            Long proveedorId, int limite,
                                                                            Solicitante solicitante) {
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        return consulta.herramientasMasAlquiladas(alcance(proveedorId, solicitante), periodo.desde(),
                periodo.hasta(), limite(limite));
    }

    @Override
    public List<ReportesResult.ClienteFrecuente> consultarClientesFrecuentes(LocalDate desde, LocalDate hasta,
                                                                             Long proveedorId, int limite,
                                                                             Solicitante solicitante) {
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        return consulta.clientesFrecuentes(alcance(proveedorId, solicitante), periodo.desde(), periodo.hasta(),
                limite(limite));
    }

    @Override
    public List<ReportesResult.Rentabilidad> consultarRentabilidad(LocalDate desde, LocalDate hasta, Long proveedorId,
                                                                   Solicitante solicitante) {
        ReportesResult.Periodo periodo = periodo(desde, hasta);
        long diasPeriodo = ChronoUnit.DAYS.between(periodo.desde(), periodo.hasta()) + 1;
        return consulta.rentabilidad(alcance(proveedorId, solicitante), periodo.desde(), periodo.hasta()).stream()
                .map(d -> {
                    BigDecimal utilidad = d.ingresos().subtract(d.costoDanos());
                    // Capacidad = cuántas unidades-día se habrían podido alquilar si todo estuviera ocupado
                    long capacidad = (long) d.stock() * diasPeriodo;
                    return new ReportesResult.Rentabilidad(d.herramientaId(), d.herramienta(), d.proveedor(),
                            d.stock(), dinero(d.ingresos()), dinero(d.costoDanos()), d.danos(), dinero(utilidad),
                            porcentaje(utilidad, d.ingresos()), d.unidadesDia(),
                            porcentaje(BigDecimal.valueOf(d.unidadesDia()), BigDecimal.valueOf(capacidad)));
                })
                .sorted(Comparator.comparing(ReportesResult.Rentabilidad::utilidad).reversed())
                .toList();
    }

    @Override
    public List<ReportesResult.Inventario> consultarInventario(Long proveedorId, Solicitante solicitante) {
        return consulta.inventario(alcance(proveedorId, solicitante), LocalDate.now(reloj)).stream()
                .map(d -> {
                    int operativas = d.stock() - d.fueraServicio();
                    int disponibles = Math.max(0, operativas - d.comprometidasHoy());
                    return new ReportesResult.Inventario(d.herramientaId(), d.herramienta(), d.categoria(),
                            d.proveedor(), d.estado(), d.stock(), d.fueraServicio(), operativas, d.comprometidasHoy(),
                            disponibles, alerta(d, operativas, disponibles));
                })
                .toList();
    }

    // ---------- Apoyo ----------

    private ReportesResult.Resumen resumen(Long alcance, ReportesResult.Periodo periodo) {
        ConsultaReportesPort.DatosResumen d = consulta.resumen(alcance, periodo.desde(), periodo.hasta());
        long intentos = d.pagosAprobados() + d.pagosRechazados();
        BigDecimal ticket = d.pagosAprobados() == 0 ? BigDecimal.ZERO
                : d.ingresosBrutos().divide(BigDecimal.valueOf(d.pagosAprobados()), 2, RoundingMode.HALF_UP);
        return new ReportesResult.Resumen(periodo, dinero(d.ingresosBrutos()), dinero(d.ingresosNetos()),
                dinero(d.iva()), d.pagosAprobados(), d.pagosRechazados(),
                porcentaje(BigDecimal.valueOf(d.pagosAprobados()), BigDecimal.valueOf(intentos)), ticket,
                d.facturasEmitidas(), dinero(d.montoFacturado()), d.reservasPorEstado(), d.clientesActivos(),
                d.herramientas(), d.unidadesTotales(), d.unidadesFueraServicio(), d.incidenciasAbiertas(),
                d.devolucionesPorValidar());
    }

    /**
     * Decide de qué proveedor son los datos. ADMIN: el que pida (o todos si no pide ninguno).
     * PROVEEDOR: siempre el suyo, ignorando lo que mande. Cualquier otro rol: sin acceso.
     */
    private Long alcance(Long proveedorIdPedido, Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return proveedorIdPedido;
        }
        if (solicitante.roles().contains(RolNombre.PROVEEDOR)) {
            return proveedorRepository.buscarPorUsuarioId(solicitante.usuarioId())
                    .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de proveedor"))
                    .getId();
        }
        throw new AccesoDenegadoException("Los reportes son solo para administradores y proveedores");
    }

    private ReportesResult.Periodo periodo(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.withDayOfYear(1) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaDeNegocioException("La fecha 'desde' (" + inicio + ") no puede ser posterior a 'hasta' ("
                    + fin + ")");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > DIAS_MAXIMOS_PERIODO) {
            throw new ReglaDeNegocioException("El periodo de un reporte puede ser de máximo 2 años");
        }
        return new ReportesResult.Periodo(inicio, fin);
    }

    private int limite(int pedido) {
        return Math.max(1, Math.min(pedido, LIMITE_MAXIMO));
    }

    private String alerta(ConsultaReportesPort.DatosInventario d, int operativas, int disponibles) {
        if (!"ACTIVA".equals(d.estado())) {
            return "Fuera del catálogo (" + d.estado() + ")";
        }
        if (operativas == 0) {
            return "Sin unidades operativas: todas fuera de servicio";
        }
        if (d.comprometidasHoy() > operativas) {
            return "Sobreasignada: hay más unidades comprometidas que operativas";
        }
        if (disponibles == 0) {
            return "Agotada hoy";
        }
        if (d.fueraServicio() > 0) {
            return d.fueraServicio() + " unidad(es) en reparación";
        }
        return null;
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
