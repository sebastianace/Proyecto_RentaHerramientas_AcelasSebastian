package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ActualizarHerramientaCommand;
import com.rentaherramientas.application.dtos.CrearHerramientaCommand;
import com.rentaherramientas.application.dtos.FiltroCatalogo;
import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.dtos.SubirImagenCommand;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

public interface GestionHerramientasUseCase {

    // ---------- Catálogo público ----------

    Pagina<HerramientaResult> listarCatalogo(FiltroCatalogo filtro, SolicitudPagina solicitud);

    /** @param solicitante puede ser null (visitante sin sesión) */
    HerramientaResult obtener(Long id, Solicitante solicitante);

    // ---------- Inventario (PROVEEDOR dueño o ADMIN) ----------

    Pagina<HerramientaResult> listarInventario(Long proveedorId, EstadoHerramienta estado, SolicitudPagina solicitud,
                                               Solicitante solicitante);

    HerramientaResult crear(CrearHerramientaCommand command, Solicitante solicitante);

    HerramientaResult actualizar(Long id, ActualizarHerramientaCommand command, Solicitante solicitante);

    HerramientaResult cambiarEstado(Long id, EstadoHerramienta estado, Solicitante solicitante);

    void inactivar(Long id, Solicitante solicitante);

    // ---------- Imágenes ----------

    HerramientaResult subirImagen(SubirImagenCommand command, Solicitante solicitante);

    HerramientaResult eliminarImagen(Long herramientaId, Long imagenId, Solicitante solicitante);

    HerramientaResult marcarImagenPrincipal(Long herramientaId, Long imagenId, Solicitante solicitante);
}
