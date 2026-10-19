package com.rentaherramientas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de la aplicación y tareas programadas.
 * Todos los casos de uso usan este Clock (zona de Bogotá) en vez de llamar a now() directamente.
 */
@Configuration
@EnableScheduling
public class TiempoConfig {

    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
