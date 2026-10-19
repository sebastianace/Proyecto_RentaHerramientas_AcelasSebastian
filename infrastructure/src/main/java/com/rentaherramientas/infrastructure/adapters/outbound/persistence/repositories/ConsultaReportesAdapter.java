package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.ReportesResult;
import com.rentaherramientas.application.ports.out.ConsultaReportesPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes con SQL nativo (solo lectura).
 *
 * <p>¿Por qué SQL y no JPA? Los reportes son agregaciones (SUM, COUNT, GROUP BY) que no corresponden a ninguna
 * entidad: con SQL la base de datos hace todo el cálculo en una consulta y solo viajan los totales, en vez de
 * cargar cientos de entidades a memoria para sumarlas en Java.</p>
 *
 * <p>Criterios usados en todas las consultas:</p>
 * <ul>
 *   <li><b>Ingresos</b>: pagos APROBADOS por su fecha de pago (criterio de caja).</li>
 *   <li><b>Alquileres</b>: reservas PAGADA, ENTREGADA o DEVUELTA (las que sí se cobraron) por su fecha de inicio.</li>
 *   <li>{@code :prov IS NULL} = todos los proveedores (vista ADMIN).</li>
 * </ul>
 */
@Repository
public class ConsultaReportesAdapter implements ConsultaReportesPort {

    // Con espacios a ambos lados a propósito: los text blocks (""") borran los espacios al final de cada línea,
    // así que "WHERE """ + ALQUILADAS quedaría pegado ("WHEREr.estado") si la constante no los trajera.
    private static final String ALQUILADAS = " r.estado IN ('PAGADA', 'ENTREGADA', 'DEVUELTA') ";
    private static final String DEL_PROVEEDOR = " (:prov IS NULL OR r.proveedor_id = :prov) ";

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReportesAdapter(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public DatosResumen resumen(Long proveedorId, LocalDate desde, LocalDate hasta) {
        MapSqlParameterSource p = parametros(proveedorId, desde, hasta);

        Map<String, Object> ingresos = jdbc.queryForMap("""
                SELECT COUNT(*) AS pagos, COALESCE(SUM(p.monto), 0) AS bruto,
                       COALESCE(SUM(r.subtotal), 0) AS neto, COALESCE(SUM(r.iva), 0) AS iva
                FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                WHERE p.estado = 'APROBADO' AND p.fecha_pago >= :desdeDt AND p.fecha_pago < :hastaDt
                  AND """ + DEL_PROVEEDOR, p);

        long rechazados = contar("""
                SELECT COUNT(*) FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                WHERE p.estado = 'RECHAZADO' AND p.fecha_pago >= :desdeDt AND p.fecha_pago < :hastaDt
                  AND """ + DEL_PROVEEDOR, p);

        Map<String, Object> facturas = jdbc.queryForMap("""
                SELECT COUNT(*) AS cantidad, COALESCE(SUM(f.total), 0) AS total
                FROM facturas f JOIN pagos p ON p.id = f.pago_id JOIN reservas r ON r.id = p.reserva_id
                WHERE f.fecha_emision >= :desdeDt AND f.fecha_emision < :hastaDt AND """ + DEL_PROVEEDOR, p);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        jdbc.query("""
                SELECT r.estado, COUNT(*) AS cantidad FROM reservas r
                WHERE r.created_at >= :desdeDt AND r.created_at < :hastaDt AND """ + DEL_PROVEEDOR + """
                 GROUP BY r.estado ORDER BY cantidad DESC""",
                p, rs -> { porEstado.put(rs.getString("estado"), rs.getLong("cantidad")); });

        long clientes = contar("""
                SELECT COUNT(DISTINCT r.cliente_id) FROM reservas r
                WHERE """ + ALQUILADAS + " AND r.fecha_inicio BETWEEN :desde AND :hasta AND " + DEL_PROVEEDOR, p);

        Map<String, Object> inventario = jdbc.queryForMap("""
                SELECT COUNT(*) AS herramientas, COALESCE(SUM(h.stock_total), 0) AS stock,
                       COALESCE(SUM(h.unidades_fuera_servicio), 0) AS fuera
                FROM herramientas h
                WHERE h.estado <> 'INACTIVA' AND (:prov IS NULL OR h.proveedor_id = :prov)""", p);

        long incidencias = contar("""
                SELECT COUNT(*) FROM reportes_danos d
                JOIN devoluciones dv ON dv.id = d.devolucion_id JOIN reservas r ON r.id = dv.reserva_id
                WHERE d.estado <> 'RESUELTO' AND """ + DEL_PROVEEDOR, p);

        long porValidar = contar("""
                SELECT COUNT(*) FROM devoluciones dv JOIN reservas r ON r.id = dv.reserva_id
                WHERE dv.validada = FALSE AND """ + DEL_PROVEEDOR, p);

        return new DatosResumen(decimal(ingresos.get("bruto")), decimal(ingresos.get("neto")),
                decimal(ingresos.get("iva")), entero(ingresos.get("pagos")), rechazados,
                entero(facturas.get("cantidad")), decimal(facturas.get("total")), porEstado, clientes,
                entero(inventario.get("herramientas")), entero(inventario.get("stock")),
                entero(inventario.get("fuera")), incidencias, porValidar);
    }

    @Override
    public List<ReportesResult.IngresoMensual> ingresosPorMes(Long proveedorId, LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                SELECT DATE_FORMAT(p.fecha_pago, '%Y-%m') AS mes, COUNT(*) AS pagos,
                       SUM(p.monto) AS bruto, SUM(r.subtotal) AS neto
                FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                WHERE p.estado = 'APROBADO' AND p.fecha_pago >= :desdeDt AND p.fecha_pago < :hastaDt
                  AND """ + DEL_PROVEEDOR + """
                 GROUP BY mes ORDER BY mes""",
                parametros(proveedorId, desde, hasta),
                (rs, i) -> new ReportesResult.IngresoMensual(rs.getString("mes"), rs.getLong("pagos"),
                        rs.getBigDecimal("bruto"), rs.getBigDecimal("neto")));
    }

    @Override
    public List<ReportesResult.IngresoProveedor> ingresosPorProveedor(LocalDate desde, LocalDate hasta) {
        // LEFT JOIN: también aparecen los proveedores sin ingresos en el periodo (con 0)
        return jdbc.query("""
                SELECT pr.id, pr.razon_social, pr.ciudad, COALESCE(i.reservas, 0) AS reservas,
                       COALESCE(i.bruto, 0) AS bruto, COALESCE(i.neto, 0) AS neto
                FROM proveedores pr
                LEFT JOIN (
                    SELECT r.proveedor_id, COUNT(*) AS reservas, SUM(p.monto) AS bruto, SUM(r.subtotal) AS neto
                    FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                    WHERE p.estado = 'APROBADO' AND p.fecha_pago >= :desdeDt AND p.fecha_pago < :hastaDt
                    GROUP BY r.proveedor_id
                ) i ON i.proveedor_id = pr.id
                ORDER BY bruto DESC, pr.razon_social""",
                parametros(null, desde, hasta),
                (rs, i) -> new ReportesResult.IngresoProveedor(rs.getLong("id"), rs.getString("razon_social"),
                        rs.getString("ciudad"), rs.getLong("reservas"), rs.getBigDecimal("bruto"),
                        rs.getBigDecimal("neto")));
    }

    @Override
    public List<ReportesResult.HerramientaAlquilada> herramientasMasAlquiladas(Long proveedorId, LocalDate desde,
                                                                               LocalDate hasta, int limite) {
        return jdbc.query("""
                SELECT h.id, h.nombre, c.nombre AS categoria, pr.razon_social AS proveedor,
                       COUNT(DISTINCT r.id) AS veces, SUM(d.cantidad) AS unidades,
                       SUM(d.cantidad * d.dias) AS unidades_dia, SUM(d.subtotal) AS ingresos
                FROM reserva_detalles d
                JOIN reservas r     ON r.id = d.reserva_id
                JOIN herramientas h ON h.id = d.herramienta_id
                JOIN categorias c   ON c.id = h.categoria_id
                JOIN proveedores pr ON pr.id = h.proveedor_id
                WHERE """ + ALQUILADAS + " AND r.fecha_inicio BETWEEN :desde AND :hasta AND " + DEL_PROVEEDOR + """
                 GROUP BY h.id, h.nombre, c.nombre, pr.razon_social
                ORDER BY veces DESC, unidades_dia DESC
                LIMIT :limite""",
                parametros(proveedorId, desde, hasta).addValue("limite", limite),
                (rs, i) -> new ReportesResult.HerramientaAlquilada(rs.getLong("id"), rs.getString("nombre"),
                        rs.getString("categoria"), rs.getString("proveedor"), rs.getLong("veces"),
                        rs.getLong("unidades"), rs.getLong("unidades_dia"), rs.getBigDecimal("ingresos")));
    }

    @Override
    public List<ReportesResult.ClienteFrecuente> clientesFrecuentes(Long proveedorId, LocalDate desde,
                                                                    LocalDate hasta, int limite) {
        return jdbc.query("""
                SELECT cl.id, CONCAT(u.nombres, ' ', u.apellidos) AS cliente, u.email, cl.ciudad,
                       COUNT(*) AS reservas, SUM(r.total) AS gastado, MAX(r.fecha_inicio) AS ultimo
                FROM reservas r
                JOIN clientes cl ON cl.id = r.cliente_id
                JOIN usuarios u  ON u.id = cl.usuario_id
                WHERE """ + ALQUILADAS + " AND r.fecha_inicio BETWEEN :desde AND :hasta AND " + DEL_PROVEEDOR + """
                 GROUP BY cl.id, u.nombres, u.apellidos, u.email, cl.ciudad
                ORDER BY reservas DESC, gastado DESC
                LIMIT :limite""",
                parametros(proveedorId, desde, hasta).addValue("limite", limite),
                (rs, i) -> new ReportesResult.ClienteFrecuente(rs.getLong("id"), rs.getString("cliente"),
                        rs.getString("email"), rs.getString("ciudad"), rs.getLong("reservas"),
                        rs.getBigDecimal("gastado"), rs.getObject("ultimo", LocalDate.class)));
    }

    @Override
    public List<DatosRentabilidad> rentabilidad(Long proveedorId, LocalDate desde, LocalDate hasta) {
        // Dos subconsultas agregadas (ingresos y daños por herramienta) unidas al inventario
        return jdbc.query("""
                SELECT h.id, h.nombre, pr.razon_social AS proveedor, h.stock_total,
                       COALESCE(i.ingresos, 0) AS ingresos, COALESCE(i.unidades_dia, 0) AS unidades_dia,
                       COALESCE(dn.costo, 0) AS costo, COALESCE(dn.cantidad, 0) AS danos
                FROM herramientas h
                JOIN proveedores pr ON pr.id = h.proveedor_id
                LEFT JOIN (
                    SELECT d.herramienta_id, SUM(d.subtotal) AS ingresos, SUM(d.cantidad * d.dias) AS unidades_dia
                    FROM reserva_detalles d JOIN reservas r ON r.id = d.reserva_id
                    WHERE """ + ALQUILADAS + """
                       AND r.fecha_inicio BETWEEN :desde AND :hasta
                    GROUP BY d.herramienta_id
                ) i ON i.herramienta_id = h.id
                LEFT JOIN (
                    SELECT rd.herramienta_id, SUM(rd.costo_estimado) AS costo, COUNT(*) AS cantidad
                    FROM reportes_danos rd JOIN devoluciones dv ON dv.id = rd.devolucion_id
                    WHERE dv.fecha_devolucion >= :desdeDt AND dv.fecha_devolucion < :hastaDt
                    GROUP BY rd.herramienta_id
                ) dn ON dn.herramienta_id = h.id
                WHERE (:prov IS NULL OR h.proveedor_id = :prov)
                  AND (h.estado <> 'INACTIVA' OR i.ingresos IS NOT NULL)""",
                parametros(proveedorId, desde, hasta),
                (rs, i) -> new DatosRentabilidad(rs.getLong("id"), rs.getString("nombre"), rs.getString("proveedor"),
                        rs.getInt("stock_total"), rs.getBigDecimal("ingresos"), rs.getBigDecimal("costo"),
                        rs.getLong("danos"), rs.getLong("unidades_dia")));
    }

    @Override
    public List<DatosInventario> inventario(Long proveedorId, LocalDate hoy) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("prov", proveedorId, Types.BIGINT)
                .addValue("hoy", Date.valueOf(hoy));
        // Comprometidas hoy: misma regla que la disponibilidad (APROBADA, PAGADA, ENTREGADA cruzando la fecha)
        return jdbc.query("""
                SELECT h.id, h.nombre, c.nombre AS categoria, pr.razon_social AS proveedor, h.estado,
                       h.stock_total, h.unidades_fuera_servicio,
                       COALESCE((SELECT SUM(d.cantidad)
                                 FROM reserva_detalles d JOIN reservas r ON r.id = d.reserva_id
                                 WHERE d.herramienta_id = h.id
                                   AND r.estado IN ('APROBADA', 'PAGADA', 'ENTREGADA')
                                   AND r.fecha_inicio <= :hoy AND r.fecha_fin >= :hoy), 0) AS comprometidas
                FROM herramientas h
                JOIN categorias c   ON c.id = h.categoria_id
                JOIN proveedores pr ON pr.id = h.proveedor_id
                WHERE (:prov IS NULL OR h.proveedor_id = :prov)
                ORDER BY h.id""",
                p,
                (rs, i) -> new DatosInventario(rs.getLong("id"), rs.getString("nombre"), rs.getString("categoria"),
                        rs.getString("proveedor"), rs.getString("estado"), rs.getInt("stock_total"),
                        rs.getInt("unidades_fuera_servicio"), rs.getInt("comprometidas")));
    }

    // ---------- Apoyo ----------

    /** Fechas inclusivas: para columnas DATETIME se busca desde el inicio de "desde" hasta antes del día siguiente a "hasta". */
    private MapSqlParameterSource parametros(Long proveedorId, LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource()
                .addValue("prov", proveedorId, Types.BIGINT)
                .addValue("desde", Date.valueOf(desde))
                .addValue("hasta", Date.valueOf(hasta))
                .addValue("desdeDt", Timestamp.valueOf(desde.atStartOfDay()))
                .addValue("hastaDt", Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
    }

    private long contar(String sql, MapSqlParameterSource p) {
        Long valor = jdbc.queryForObject(sql, p, Long.class);
        return valor == null ? 0 : valor;
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static long entero(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }
}
