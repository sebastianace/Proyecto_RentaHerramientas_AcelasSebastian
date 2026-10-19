package com.rentaherramientas.domain.exceptions;

/** Se lanza cuando la operación choca con datos existentes (email o documento repetido). Se traduce a HTTP 409. */
public class ConflictoException extends DominioException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
