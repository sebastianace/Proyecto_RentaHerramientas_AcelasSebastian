package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.IdentidadEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador de salida: implementa el puerto del dominio usando Spring Data JPA. */
@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;
    private final RolResolver rolResolver;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpa, RolResolver rolResolver) {
        this.jpa = jpa;
        this.rolResolver = rolResolver;
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

    /** Carga la entidad existente y le copia los cambios (contraseña, datos personales, activo, roles). */
    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entidad = jpa.findById(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuario.getId()));
        IdentidadEntityMapper.copiar(usuario, entidad, rolResolver::buscar);
        return IdentidadEntityMapper.aDominio(jpa.saveAndFlush(entidad));
    }
}
