package com.rentaherramientas.domain.exceptions;

/**
 * Excepción base de todas las reglas de negocio violadas en el dominio.
 * Es unchecked (RuntimeException) para no ensuciar las firmas de los métodos.
 */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensaje) {
        super(mensaje);
    }
}
