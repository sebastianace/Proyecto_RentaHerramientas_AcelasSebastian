package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.TokenGenerado;
import com.rentaherramientas.domain.model.aggregates.Usuario;

/** Genera el token de acceso de un usuario. En infraestructura se implementa con JWT. */
public interface TokenProviderPort {

    TokenGenerado generar(Usuario usuario);
}
