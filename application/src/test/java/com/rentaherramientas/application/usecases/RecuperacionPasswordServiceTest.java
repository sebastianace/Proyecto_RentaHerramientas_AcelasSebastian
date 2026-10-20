package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.config.PoliticaRecuperacion;
import com.rentaherramientas.application.ports.out.EnvioCorreoPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.TokenRecuperacion;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.TokenRecuperacionRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Recuperación de contraseña con los puertos simulados. */
@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneId.of("America/Bogota"));
    static final String URL = "http://localhost:5500/restablecer.html";

    @Mock UsuarioRepository usuarioRepository;
    @Mock TokenRecuperacionRepository tokenRepository;
    @Mock PasswordEncoderPort passwordEncoder;
    @Mock EnvioCorreoPort correo;
    RecuperacionPasswordService servicio;

    final Usuario usuario = Usuario.reconstruir(23L, new Email("juanpablo.ardila@gmail.com"), "$2a$hashViejo",
            "Juan Pablo", "Ardila", "3158735513", true, Set.of(RolNombre.CLIENTE));

    @BeforeEach
    void setUp() {
        servicio = new RecuperacionPasswordService(usuarioRepository, tokenRepository, passwordEncoder, correo,
                new PoliticaRecuperacion(Duration.ofMinutes(30), URL), RELOJ);
    }

    @Test
    @DisplayName("Si el correo no existe no se envía nada y tampoco se lanza error (no se revela)")
    void correoInexistente() {
        when(usuarioRepository.buscarPorEmail(any())).thenReturn(Optional.empty());

        servicio.solicitarRecuperacion("nadie@gmail.com");

        verifyNoInteractions(correo, tokenRepository);
    }

    @Test
    @DisplayName("Envía un enlace con el token y en la base guarda solo su hash, con 30 minutos de vigencia")
    void solicitar() {
        when(usuarioRepository.buscarPorEmail(any())).thenReturn(Optional.of(usuario));

        servicio.solicitarRecuperacion("juanpablo.ardila@gmail.com");

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo).enviarRecuperacionPassword(eq("juanpablo.ardila@gmail.com"), eq("Juan Pablo"), enlace.capture(),
                eq(Duration.ofMinutes(30)));
        String token = enlace.getValue().substring((URL + "?token=").length());
        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).invalidarPendientes(23L);
        verify(tokenRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getTokenHash()).isEqualTo(RecuperacionPasswordService.hash(token))
                .isNotEqualTo(token).hasSize(64);
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(LocalDateTime.now(RELOJ).plusMinutes(30));
    }

    @Test
    @DisplayName("Con un token válido cambia la contraseña (cifrada) y marca el token como usado")
    void restablecer() {
        TokenRecuperacion token = TokenRecuperacion.emitir(23L, RecuperacionPasswordService.hash("abc"),
                LocalDateTime.now(RELOJ), Duration.ofMinutes(30));
        when(tokenRepository.buscarPorHash(RecuperacionPasswordService.hash("abc"))).thenReturn(Optional.of(token));
        when(usuarioRepository.buscarPorId(23L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.cifrar("NuevaClave2026")).thenReturn("$2a$hashNuevo");

        servicio.restablecerPassword("abc", "NuevaClave2026");

        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$hashNuevo");
        assertThat(token.isUsado()).isTrue();
        verify(usuarioRepository).guardar(usuario);
        verify(tokenRepository).guardar(token);
    }

    @Test
    @DisplayName("Un token que no existe se rechaza sin tocar ninguna contraseña")
    void tokenInvalido() {
        when(tokenRepository.buscarPorHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.restablecerPassword("inventado", "NuevaClave2026"))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(usuarioRepository, never()).guardar(any());
    }
}
