package com.rentaherramientas.domain.exceptions;

/** Se lanza cuando se busca un recurso por su identificador y no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends DominioException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con id " + id + " no existe");
    }
}
