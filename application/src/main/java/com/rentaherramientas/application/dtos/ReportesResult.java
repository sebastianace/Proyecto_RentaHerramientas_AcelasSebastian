package com.rentaherramientas.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Resultados del panel de reportes. Se agrupan en una sola clase porque son "fotos" de solo lectura,
 * sin comportamiento: cada record es la forma JSON de un reporte.
 *
 * <p>Ingresos: se cuentan los pagos APROBADOS por su fecha de pago (criterio de caja).
 * "Brutos" incluyen el IVA; "netos" no, porque el IVA se recauda para la DIAN y no es ingreso del negocio.</p>
 */
public final class ReportesResult {

    private ReportesResult() {
    }

    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    /** Tarjetas de métricas del panel (KPIs). Si lo pide un PROVEEDOR, todo es solo de su inventario. */
    public record Resumen(
            Periodo periodo,
            BigDecimal ingresosBrutos,
            BigDecimal ingresosNetos,
            BigDecimal ivaRecaudado,
            long pagosAprobados,
            long pagosRechazados,
            BigDecimal tasaAprobacionPagos,
            BigDecimal ticketPromedio,
            long facturasEmitidas,
            BigDecimal montoFacturado,
            Map<String, Long> reservasPorEstado,
            long clientesActivos,
            long herramientas,
            long unidadesTotales,
            long unidadesFueraServicio,
            long incidenciasAbiertas,
            long devolucionesPorValidar) {
    }

    public record IngresoMensual(String mes, long pagos, BigDecimal ingresosBrutos, BigDecimal ingresosNetos) {
    }

    public record IngresoProveedor(Long proveedorId, String proveedor, String ciudad, long reservasPagadas,
                                   BigDecimal ingresosBrutos, BigDecimal ingresosNetos) {
    }

    public record HerramientaAlquilada(Long herramientaId, String herramienta, String categoria, String proveedor,
                                       long vecesAlquilada, long unidadesAlquiladas, long unidadesDia,
                                       BigDecimal ingresos) {
    }

    public record ClienteFrecuente(Long clienteId, String cliente, String email, String ciudad, long reservas,
                                   BigDecimal totalGastado, LocalDate ultimoAlquiler) {
    }

    /**
     * Rentabilidad por herramienta en el periodo.
     *
     * @param utilidad    ingresos − costo estimado de los daños
     * @param margen      utilidad / ingresos, en porcentaje
     * @param ocupacion   unidades-día alquiladas / (stock × días del periodo), en porcentaje
     */
    public record Rentabilidad(Long herramientaId, String herramienta, String proveedor, int stock,
                               BigDecimal ingresos, BigDecimal costoDanos, long danos, BigDecimal utilidad,
                               BigDecimal margen, long unidadesDia, BigDecimal ocupacion) {
    }

    /** Estado y disponibilidad HOY de cada herramienta, con una alerta legible si hay algo que atender. */
    public record Inventario(Long herramientaId, String herramienta, String categoria, String proveedor,
                             String estado, int stock, int fueraServicio, int operativas, int comprometidasHoy,
                             int disponiblesHoy, String alerta) {
    }

    /** Todo el panel en una sola respuesta (el frontend lo carga con una petición). */
    public record Panel(Resumen resumen, List<IngresoMensual> ingresosPorMes,
                        List<HerramientaAlquilada> masAlquiladas, List<ClienteFrecuente> clientesFrecuentes) {
    }
}
