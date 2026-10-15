package com.rentaherramientas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que se ejecuta una vez por petición: lee el header "Authorization: Bearer &lt;token&gt;",
 * valida el JWT y, si es correcto, registra al usuario en el SecurityContext.
 *
 * <p>No responde errores: si no hay token o es inválido simplemente deja seguir la petición
 * sin autenticar. Quien decide si la ruta exige autenticación es la configuración de
 * SecurityConfig.</p>
 */
public class JwtValidationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtValidationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIJO)) {
            String token = header.substring(PREFIJO.length()).trim();
            tokenProvider.validar(token).ifPresent(usuario -> {
                UsernamePasswordAuthenticationToken autenticacion =
                        UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.authorities());
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            });
        }
        chain.doFilter(request, response);
    }
}
