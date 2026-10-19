package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ReportesResult;
import com.rentaherramientas.application.dtos.Solicitante;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel de reportes y estadísticas. ADMIN ve toda la plataforma (y puede filtrar por proveedor);
 * PROVEEDOR ve solo su inventario; CLIENTE no tiene acceso.
 *
 * <p>Fechas opcionales: si no llegan, el periodo es desde el 1 de enero del año en curso hasta hoy.</p>
 */
public interface ReportesUseCase {

    ReportesResult.Panel consultarPanel(LocalDate desde, LocalDate hasta, Long proveedorId, Solicitante solicitante);

    ReportesResult.Resumen consultarResumen(LocalDate desde, LocalDate hasta, Long proveedorId,
                                            Solicitante solicitante);

    List<ReportesResult.IngresoMensual> consultarIngresosPorMes(LocalDate desde, LocalDate hasta, Long proveedorId,
                                                                Solicitante solicitante);

    /** Solo ADMIN: comparativo de ingresos entre proveedores. */
    List<ReportesResult.IngresoProveedor> consultarIngresosPorProveedor(LocalDate desde, LocalDate hasta,
                                                                        Solicitante solicitante);

    List<ReportesResult.HerramientaAlquilada> consultarMasAlquiladas(LocalDate desde, LocalDate hasta,
                                                                     Long proveedorId, int limite,
                                                                     Solicitante solicitante);

    List<ReportesResult.ClienteFrecuente> consultarClientesFrecuentes(LocalDate desde, LocalDate hasta,
                                                                      Long proveedorId, int limite,
                                                                      Solicitante solicitante);

    List<ReportesResult.Rentabilidad> consultarRentabilidad(LocalDate desde, LocalDate hasta, Long proveedorId,
                                                            Solicitante solicitante);

    List<ReportesResult.Inventario> consultarInventario(Long proveedorId, Solicitante solicitante);
}
