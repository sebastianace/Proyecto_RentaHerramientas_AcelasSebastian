package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.config.PoliticaRecuperacion;
import com.rentaherramientas.application.ports.in.RecuperacionPasswordUseCase;
import com.rentaherramientas.application.ports.out.EnvioCorreoPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.TokenRecuperacion;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.repositories.TokenRecuperacionRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperación de contraseña olvidada.
 *
 * <p><b>Paso 1, solicitar:</b> se genera un token aleatorio de 256 bits ({@link SecureRandom}), se guarda solo su
 * hash SHA-256 con vencimiento, y se envía el enlace al correo. Si el correo no existe se responde igual, para no
 * revelar qué correos están registrados.</p>
 *
 * <p><b>Paso 2, restablecer:</b> se calcula el hash del token recibido, se busca, se consume (falla si venció o ya se
 * usó) y se guarda la clave nueva cifrada con BCrypt. Los dos cambios van en la misma transacción.</p>
 */
public class RecuperacionPasswordService implements RecuperacionPasswordUseCase {

    private static final int BYTES_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final EnvioCorreoPort correo;
    private final PoliticaRecuperacion politica;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository, TokenRecuperacionRepository tokenRepository,
                                       PasswordEncoderPort passwordEncoder, EnvioCorreoPort correo,
                                       PoliticaRecuperacion politica, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.politica = politica;
        this.reloj = reloj;
    }

    @Override
    public void solicitarRecuperacion(String email) {
        Optional<Usuario> usuario = usuarioRepository.buscarPorEmail(new Email(email));
        if (usuario.isEmpty() || !usuario.get().isActivo()) {
            return; // Misma respuesta exista o no: no se revela si el correo está registrado
        }
        Usuario u = usuario.get();
        String token = generarToken();
        tokenRepository.invalidarPendientes(u.getId());
        tokenRepository.guardar(TokenRecuperacion.emitir(u.getId(), hash(token), LocalDateTime.now(reloj),
                politica.vigencia()));
        correo.enviarRecuperacionPassword(u.getEmail().valor(), u.getNombres(),
                politica.urlRestablecer() + "?token=" + token, politica.vigencia());
    }

    @Override
    public void restablecerPassword(String token, String nuevaPassword) {
        if (token == null || token.isBlank()) {
            throw new ReglaDeNegocioException("El enlace de recuperación no es válido");
        }
        TokenRecuperacion recuperacion = tokenRepository.buscarPorHash(hash(token.trim()))
                .orElseThrow(() -> new ReglaDeNegocioException("El enlace de recuperación no es válido o ya venció"));
        recuperacion.usar(LocalDateTime.now(reloj));
        Usuario usuario = usuarioRepository.buscarPorId(recuperacion.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", recuperacion.getUsuarioId()));
        usuario.cambiarPassword(passwordEncoder.cifrar(nuevaPassword));
        usuarioRepository.guardar(usuario);
        tokenRepository.guardar(recuperacion);
    }

    /** 32 bytes aleatorios en Base64 apto para URL (sin '+', '/' ni '='). */
    private String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e); // Java siempre lo incluye
        }
    }
}
