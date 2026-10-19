package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;

import java.util.Optional;

/** Puerto de salida para consultar usuarios. Lo implementa un adaptador JPA en infrastructure. */
public interface UsuarioRepository {

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(Email email);

    boolean existePorEmail(Email email);
}
