package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.ReportesResult;
import com.rentaherramientas.application.ports.in.ReportesUseCase;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel de reportes y estadísticas. Todas las rutas son de solo lectura y exigen ADMIN o PROVEEDOR;
 * el caso de uso se encarga de que un PROVEEDOR vea únicamente su propio inventario.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
@Tag(name = "Reportes", description = "Ingresos, herramientas más alquiladas, clientes frecuentes, rentabilidad e inventario. "
        + "Fechas opcionales (por defecto: 1 de enero del año en curso hasta hoy). "
        + "El PROVEEDOR solo ve su inventario; el ADMIN ve todo y puede filtrar con proveedorId")
public class ReporteController {

    private final ReportesUseCase reportes;

    public ReporteController(ReportesUseCase reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/panel")
    @Operation(summary = "Panel completo en una sola petición",
            description = "Resumen + ingresos por mes + top 5 herramientas + top 5 clientes")
    public ReportesResult.Panel panel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarPanel(desde, hasta, proveedorId, usuario.aSolicitante());
    }

    @GetMapping("/resumen")
    @Operation(summary = "Métricas clave (KPIs)",
            description = "Ingresos brutos y netos, IVA, pagos, facturación, reservas por estado, inventario e incidencias")
    public ReportesResult.Resumen resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarResumen(desde, hasta, proveedorId, usuario.aSolicitante());
    }

    @GetMapping("/ingresos")
    @Operation(summary = "Ingresos por mes", description = "Pagos aprobados agrupados por mes de pago")
    public List<ReportesResult.IngresoMensual> ingresosPorMes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarIngresosPorMes(desde, hasta, proveedorId, usuario.aSolicitante());
    }

    @GetMapping("/ingresos/proveedores")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingresos por proveedor (solo ADMIN)", description = "Ranking comparativo de proveedores")
    public List<ReportesResult.IngresoProveedor> ingresosPorProveedor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarIngresosPorProveedor(desde, hasta, usuario.aSolicitante());
    }

    @GetMapping("/herramientas-mas-alquiladas")
    @Operation(summary = "Herramientas más alquiladas",
            description = "Ordenadas por número de alquileres; incluye unidades-día e ingresos")
    public List<ReportesResult.HerramientaAlquilada> masAlquiladas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarMasAlquiladas(desde, hasta, proveedorId, limite, usuario.aSolicitante());
    }

    @GetMapping("/clientes-frecuentes")
    @Operation(summary = "Clientes frecuentes", description = "Ordenados por número de alquileres y monto gastado")
    public List<ReportesResult.ClienteFrecuente> clientesFrecuentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarClientesFrecuentes(desde, hasta, proveedorId, limite, usuario.aSolicitante());
    }

    @GetMapping("/rentabilidad")
    @Operation(summary = "Rentabilidad por herramienta",
            description = "Ingresos − costo de daños = utilidad; margen (%) y ocupación (%) del periodo")
    public List<ReportesResult.Rentabilidad> rentabilidad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarRentabilidad(desde, hasta, proveedorId, usuario.aSolicitante());
    }

    @GetMapping("/inventario")
    @Operation(summary = "Disponibilidad y estado de las herramientas hoy",
            description = "Stock, unidades fuera de servicio, comprometidas y disponibles hoy, con alertas")
    public List<ReportesResult.Inventario> inventario(
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reportes.consultarInventario(proveedorId, usuario.aSolicitante());
    }
}
