package com.rentaherramientas.application.ports.out;

/**
 * Verifica email y clave. Si son incorrectos lanza CredencialesInvalidasException,
 * y si el usuario está desactivado lanza UsuarioInactivoException.
 * En infraestructura lo implementa el AuthenticationManager de Spring Security.
 */
public interface AutenticadorPort {

    void autenticar(String email, String password);
}
