package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.ReportesResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Puerto de lectura de los reportes (CQRS: el lado de lectura no pasa por los agregados).
 * Los reportes son agregaciones (SUM, COUNT, GROUP BY) de varias tablas; la implementación decide cómo
 * calcularlas (en este proyecto, SQL nativo en ConsultaReportesAdapter).
 *
 * <p>En todos los métodos {@code proveedorId == null} significa "todos los proveedores" (vista del ADMIN).
 * Las fechas {@code desde} y {@code hasta} son inclusivas. El puerto entrega cifras crudas; los indicadores
 * derivados (margen, ocupación, ticket promedio) los calcula el caso de uso.</p>
 */
public interface ConsultaReportesPort {

    /** Cifras crudas del resumen; el caso de uso calcula los indicadores derivados. */
    record DatosResumen(BigDecimal ingresosBrutos, BigDecimal ingresosNetos, BigDecimal iva, long pagosAprobados,
                        long pagosRechazados, long facturasEmitidas, BigDecimal montoFacturado,
                        Map<String, Long> reservasPorEstado, long clientesActivos, long herramientas,
                        long unidadesTotales, long unidadesFueraServicio, long incidenciasAbiertas,
                        long devolucionesPorValidar) {
    }

    record DatosRentabilidad(Long herramientaId, String herramienta, String proveedor, int stock,
                             BigDecimal ingresos, BigDecimal costoDanos, long danos, long unidadesDia) {
    }

    record DatosInventario(Long herramientaId, String herramienta, String categoria, String proveedor, String estado,
                           int stock, int fueraServicio, int comprometidasHoy) {
    }

    DatosResumen resumen(Long proveedorId, LocalDate desde, LocalDate hasta);

    List<ReportesResult.IngresoMensual> ingresosPorMes(Long proveedorId, LocalDate desde, LocalDate hasta);

    List<ReportesResult.IngresoProveedor> ingresosPorProveedor(LocalDate desde, LocalDate hasta);

    List<ReportesResult.HerramientaAlquilada> herramientasMasAlquiladas(Long proveedorId, LocalDate desde,
                                                                        LocalDate hasta, int limite);

    List<ReportesResult.ClienteFrecuente> clientesFrecuentes(Long proveedorId, LocalDate desde, LocalDate hasta,
                                                             int limite);

    List<DatosRentabilidad> rentabilidad(Long proveedorId, LocalDate desde, LocalDate hasta);

    List<DatosInventario> inventario(Long proveedorId, LocalDate hoy);
}
