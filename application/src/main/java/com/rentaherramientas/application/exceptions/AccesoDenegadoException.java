package com.rentaherramientas.application.exceptions;

/** El usuario está autenticado pero intenta ver o modificar datos que no son suyos. Se traduce a HTTP 403. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
