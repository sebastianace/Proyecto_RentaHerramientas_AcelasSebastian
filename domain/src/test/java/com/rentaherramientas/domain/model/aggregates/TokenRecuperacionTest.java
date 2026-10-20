package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenRecuperacionTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 10, 0);

    private TokenRecuperacion emitido() {
        return TokenRecuperacion.emitir(23L, "a".repeat(64), AHORA, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Se puede usar dentro de los 30 minutos y queda marcado como usado")
    void usarDentroDelPlazo() {
        TokenRecuperacion token = emitido();
        token.usar(AHORA.plusMinutes(29));
        assertThat(token.isUsado()).isTrue();
        assertThat(token.getExpiraEn()).isEqualTo(AHORA.plusMinutes(30));
    }

    @Test
    @DisplayName("El mismo enlace no sirve dos veces")
    void usoUnico() {
        TokenRecuperacion token = emitido();
        token.usar(AHORA);
        assertThatThrownBy(() -> token.usar(AHORA.plusMinutes(1)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya fue usado");
    }

    @Test
    @DisplayName("Después del vencimiento ya no sirve")
    void vencido() {
        assertThatThrownBy(() -> emitido().usar(AHORA.plusMinutes(31)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("venció");
    }
}
