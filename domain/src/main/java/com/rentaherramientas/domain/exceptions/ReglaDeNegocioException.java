package com.rentaherramientas.domain.exceptions;

/** Se lanza cuando un dato o una operación viola una regla del negocio. Se traduce a HTTP 422. */
public class ReglaDeNegocioException extends DominioException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }

    /** Valida que un texto obligatorio venga con contenido y lo devuelve sin espacios sobrantes. */
    public static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    /** Normaliza un texto opcional: null o vacío se guardan como null. */
    public static String opcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
