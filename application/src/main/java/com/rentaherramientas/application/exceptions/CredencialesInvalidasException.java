package com.rentaherramientas.application.exceptions;

/**
 * Email o clave incorrectos. El mensaje es el mismo en ambos casos a propósito:
 * así un atacante no puede averiguar qué emails están registrados. Se traduce a HTTP 401.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
