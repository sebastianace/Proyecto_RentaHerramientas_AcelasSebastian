package com.rentaherramientas.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * <ul>
 *   <li><b>Stateless:</b> no hay sesión en el servidor; cada petición trae su JWT.</li>
 *   <li><b>CSRF deshabilitado:</b> CSRF ataca cookies de sesión; como usamos tokens en el header, no aplica.</li>
 *   <li><b>Reglas por URL:</b> login, registro, Swagger y la consulta del catálogo son públicos; el resto exige token.</li>
 *   <li><b>Reglas por rol:</b> con @PreAuthorize en cada endpoint (@EnableMethodSecurity).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String[] RUTAS_SWAGGER = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};
    private static final String[] RUTAS_CATALOGO_PUBLICO = {
            "/api/v1/herramientas", "/api/v1/herramientas/**",
            "/api/v1/categorias", "/api/v1/categorias/**",
            "/media/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro").permitAll()
                        // Cotizar es público, como en cualquier tienda: solo calcula con precios públicos, no guarda nada
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas/cotizacion").permitAll()
                        // El orden importa: la primera regla que coincide gana. El inventario va ANTES
                        // que la regla pública de herramientas porque "/inventario" también calza con "/**"
                        .requestMatchers(HttpMethod.GET, "/api/v1/herramientas/inventario").authenticated()
                        .requestMatchers(HttpMethod.GET, RUTAS_CATALOGO_PUBLICO).permitAll()
                        .requestMatchers(RUTAS_SWAGGER).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtValidationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt: algoritmo lento a propósito y con "sal" aleatoria; dos claves iguales generan hashes distintos. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Spring arma el AuthenticationManager con nuestro UserDetailsService y el PasswordEncoder. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** CORS: permite que el frontend (otro origen, ej. Live Server en :5500) llame a la API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties propiedades) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(propiedades.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location", "Content-Disposition"));
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
