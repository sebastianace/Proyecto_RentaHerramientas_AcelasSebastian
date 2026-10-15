package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.FiltroCatalogo;
import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Optional;

/**
 * Puerto de CONSULTA (lado de lectura). Las escrituras pasan por el agregado Herramienta,
 * pero para leer el catálogo conviene una consulta optimizada que traiga en un solo viaje
 * la herramienta con su categoría, proveedor e imágenes. Es una separación ligera estilo CQRS.
 */
public interface ConsultaHerramientasPort {

    Optional<HerramientaResult> obtener(Long id);

    /** Solo herramientas ACTIVAS de proveedores activos. */
    Pagina<HerramientaResult> buscarCatalogo(FiltroCatalogo filtro, SolicitudPagina solicitud);

    /** Inventario completo, en cualquier estado. proveedorId y estado son opcionales (null = todos). */
    Pagina<HerramientaResult> listarInventario(Long proveedorId, EstadoHerramienta estado, SolicitudPagina solicitud);
}
