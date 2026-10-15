package com.rentaherramientas.infrastructure.security;

import com.rentaherramientas.application.exceptions.CredencialesInvalidasException;
import com.rentaherramientas.application.exceptions.UsuarioInactivoException;
import com.rentaherramientas.application.ports.out.AutenticadorPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto AutenticadorPort usando el AuthenticationManager de Spring Security.
 * Traduce las excepciones de Spring a excepciones de la aplicación, para que los casos de uso
 * no dependan de Spring Security.
 */
@Component
public class SpringSecurityAutenticadorAdapter implements AutenticadorPort {

    private final AuthenticationManager authenticationManager;

    public SpringSecurityAutenticadorAdapter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void autenticar(String email, String password) {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (DisabledException e) {
            throw new UsuarioInactivoException();
        } catch (AuthenticationException e) {
            throw new CredencialesInvalidasException();
        }
    }
}
