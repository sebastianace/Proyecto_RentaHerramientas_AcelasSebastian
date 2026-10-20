package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.TokenRecuperacion;

import java.util.Optional;

/** Puerto de persistencia de los tokens de recuperación de contraseña. */
public interface TokenRecuperacionRepository {

    TokenRecuperacion guardar(TokenRecuperacion token);

    Optional<TokenRecuperacion> buscarPorHash(String tokenHash);

    /** Anula los enlaces pendientes de un usuario: al pedir uno nuevo, los anteriores dejan de servir. */
    void invalidarPendientes(Long usuarioId);
}
