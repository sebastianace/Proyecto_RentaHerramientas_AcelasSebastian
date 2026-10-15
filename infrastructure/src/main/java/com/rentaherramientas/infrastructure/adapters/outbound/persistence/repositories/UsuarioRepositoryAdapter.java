package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.IdentidadEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador de salida: implementa el puerto del dominio usando Spring Data JPA. */
@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(Email email) {
        return jpa.findByEmail(email.valor()).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public boolean existePorEmail(Email email) {
        return jpa.existsByEmail(email.valor());
    }
}
