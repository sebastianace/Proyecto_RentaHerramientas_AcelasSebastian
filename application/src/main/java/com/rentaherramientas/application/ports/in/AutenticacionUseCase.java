package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.AutenticacionResult;
import com.rentaherramientas.application.dtos.LoginCommand;
import com.rentaherramientas.application.dtos.PerfilResult;
import com.rentaherramientas.application.dtos.RegistrarClienteCommand;

/** Puerto de entrada: inicio de sesión, registro público de clientes y perfil propio. */
public interface AutenticacionUseCase {

    AutenticacionResult iniciarSesion(LoginCommand command);

    /** Registro abierto: cualquier persona puede crear su cuenta de cliente y queda con sesión iniciada. */
    AutenticacionResult registrarCliente(RegistrarClienteCommand command);

    PerfilResult obtenerPerfil(Long usuarioId);
}
