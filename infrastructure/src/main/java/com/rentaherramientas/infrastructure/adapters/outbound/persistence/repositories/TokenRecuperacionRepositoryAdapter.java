package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.TokenRecuperacion;
import com.rentaherramientas.domain.repositories.TokenRecuperacionRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.TokenRecuperacionEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador de salida: implementa el puerto del dominio con Spring Data JPA. */
@Repository
public class TokenRecuperacionRepositoryAdapter implements TokenRecuperacionRepository {

    private final TokenRecuperacionJpaRepository jpa;

    public TokenRecuperacionRepositoryAdapter(TokenRecuperacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TokenRecuperacion guardar(TokenRecuperacion token) {
        TokenRecuperacionEntity entidad = token.getId() == null ? new TokenRecuperacionEntity()
                : jpa.findById(token.getId())
                     .orElseThrow(() -> new RecursoNoEncontradoException("Token de recuperación", token.getId()));
        entidad.setUsuarioId(token.getUsuarioId());
        entidad.setTokenHash(token.getTokenHash());
        entidad.setExpiraEn(token.getExpiraEn());
        entidad.setUsado(token.isUsado());
        return aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<TokenRecuperacion> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(TokenRecuperacionRepositoryAdapter::aDominio);
    }

    @Override
    public void invalidarPendientes(Long usuarioId) {
        jpa.invalidarPendientes(usuarioId);
    }

    private static TokenRecuperacion aDominio(TokenRecuperacionEntity e) {
        return TokenRecuperacion.reconstruir(e.getId(), e.getUsuarioId(), e.getTokenHash(), e.getExpiraEn(),
                e.isUsado());
    }
}
