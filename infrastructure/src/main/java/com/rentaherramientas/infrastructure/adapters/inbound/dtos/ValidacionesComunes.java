package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

/** Expresiones regulares compartidas por los DTOs de entrada. */
final class ValidacionesComunes {

    /** 8 a 72 caracteres (72 es el límite de BCrypt), al menos una letra y un número. */
    static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    static final String MENSAJE_PASSWORD = "La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un número";

    static final String REGEX_TELEFONO = "^[0-9+ ]{7,20}$";
    static final String MENSAJE_TELEFONO = "El teléfono solo puede tener números, espacios o +, entre 7 y 20 caracteres";

    private ValidacionesComunes() {
    }
}
