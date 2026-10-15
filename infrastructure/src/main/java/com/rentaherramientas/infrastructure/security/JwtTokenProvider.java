package com.rentaherramientas.infrastructure.security;

import com.rentaherramientas.application.dtos.TokenGenerado;
import com.rentaherramientas.application.ports.out.TokenProviderPort;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Genera y valida los JWT (adaptador del puerto TokenProviderPort).
 *
 * <p>Estructura del token: header.payload.firma. El payload lleva el email (sub), el id del
 * usuario (uid) y sus roles. La firma HMAC-SHA con la clave secreta garantiza que nadie
 * pueda modificar el payload sin que se note.</p>
 */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int BYTES_MINIMOS_CLAVE = 32;
    private static final String CLAIM_USUARIO_ID = "uid";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey clave;
    private final Duration expiracion;
    private final String emisor;

    public JwtTokenProvider(JwtProperties propiedades) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(propiedades.secret());
        } catch (RuntimeException e) {
            throw new IllegalStateException("app.jwt.secret no es un texto Base64 válido. Revisa JWT_SECRET en el .env", e);
        }
        if (bytes.length < BYTES_MINIMOS_CLAVE) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos " + BYTES_MINIMOS_CLAVE
                    + " bytes en Base64 (256 bits). Revisa JWT_SECRET en el .env");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracion = propiedades.expiracion();
        this.emisor = propiedades.emisor();
    }

    @Override
    public TokenGenerado generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(expiracion);
        String token = Jwts.builder()
                .subject(usuario.getEmail().valor())
                .issuer(emisor)
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROLES, usuario.getRoles().stream().map(Enum::name).sorted().toList())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiraEn))
                .signWith(clave)
                .compact();
        return new TokenGenerado(token, expiraEn);
    }

    /**
     * Valida firma, emisor y expiración. Si algo falla devuelve vacío (la petición queda
     * sin autenticar y, si la ruta es protegida, el EntryPoint responde 401).
     */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long usuarioId = ((Number) claims.get(CLAIM_USUARIO_ID)).longValue();
            List<?> roles = claims.get(CLAIM_ROLES, List.class);
            return Optional.of(new UsuarioAutenticado(usuarioId, claims.getSubject(), UsuarioAutenticado.rolesDesde(roles)));
        } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
            log.debug("Token JWT rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
