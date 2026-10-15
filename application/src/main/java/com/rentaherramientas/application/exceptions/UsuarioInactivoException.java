package com.rentaherramientas.application.exceptions;

/** El usuario existe pero fue desactivado por un administrador. Se traduce a HTTP 403. */
public class UsuarioInactivoException extends RuntimeException {

    public UsuarioInactivoException() {
        super("El usuario está inactivo. Contacta al administrador");
    }
}
