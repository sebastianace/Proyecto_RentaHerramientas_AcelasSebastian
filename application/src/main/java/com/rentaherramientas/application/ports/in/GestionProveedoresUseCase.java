package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ActualizarProveedorCommand;
import com.rentaherramientas.application.dtos.ProveedorResult;
import com.rentaherramientas.application.dtos.RegistrarProveedorCommand;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

/** Puerto de entrada: CRUD de proveedores (los registra el administrador). */
public interface GestionProveedoresUseCase {

    ProveedorResult crear(RegistrarProveedorCommand command);

    ProveedorResult obtener(Long id, Solicitante solicitante);

    Pagina<ProveedorResult> listar(SolicitudPagina solicitud);

    ProveedorResult actualizar(Long id, ActualizarProveedorCommand command, Solicitante solicitante);

    void desactivar(Long id);

    void activar(Long id);
}
