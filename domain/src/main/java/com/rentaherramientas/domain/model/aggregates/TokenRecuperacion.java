package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Token para recuperar la contraseña. Vive en el dominio porque tiene reglas propias:
 * vence a los pocos minutos y se puede usar UNA sola vez.
 *
 * <p>Solo se guarda el hash (SHA-256) del token, nunca el token en sí: si alguien leyera la base de datos,
 * no podría usar los enlaces pendientes.</p>
 */
public class TokenRecuperacion {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final LocalDateTime expiraEn;
    private boolean usado;

    private TokenRecuperacion(Long id, Long usuarioId, String tokenHash, LocalDateTime expiraEn, boolean usado) {
        if (usuarioId == null || expiraEn == null) {
            throw new ReglaDeNegocioException("El token de recuperación requiere usuario y fecha de vencimiento");
        }
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = ReglaDeNegocioException.requerido(tokenHash, "tokenHash");
        this.expiraEn = expiraEn;
        this.usado = usado;
    }

    public static TokenRecuperacion emitir(Long usuarioId, String tokenHash, LocalDateTime ahora, Duration vigencia) {
        return new TokenRecuperacion(null, usuarioId, tokenHash, ahora.plus(vigencia), false);
    }

    public static TokenRecuperacion reconstruir(Long id, Long usuarioId, String tokenHash, LocalDateTime expiraEn,
                                                boolean usado) {
        return new TokenRecuperacion(id, usuarioId, tokenHash, expiraEn, usado);
    }

    /** Consume el token. Falla si ya se usó o si venció: el mismo enlace no sirve dos veces. */
    public void usar(LocalDateTime ahora) {
        if (usado) {
            throw new ReglaDeNegocioException("Este enlace de recuperación ya fue usado. Solicita uno nuevo");
        }
        if (ahora.isAfter(expiraEn)) {
            throw new ReglaDeNegocioException("El enlace de recuperación venció. Solicita uno nuevo");
        }
        this.usado = true;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public boolean isUsado() { return usado; }
}
