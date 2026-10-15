package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ActualizarClienteCommand;
import com.rentaherramientas.application.dtos.ClienteResult;
import com.rentaherramientas.application.dtos.RegistrarClienteCommand;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

/** Puerto de entrada: CRUD de clientes. */
public interface GestionClientesUseCase {

    ClienteResult crear(RegistrarClienteCommand command);

    ClienteResult obtener(Long id, Solicitante solicitante);

    Pagina<ClienteResult> listar(SolicitudPagina solicitud);

    ClienteResult actualizar(Long id, ActualizarClienteCommand command, Solicitante solicitante);

    void desactivar(Long id);

    void activar(Long id);
}
