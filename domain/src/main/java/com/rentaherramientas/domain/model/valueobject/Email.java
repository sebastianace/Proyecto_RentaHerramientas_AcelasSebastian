package com.rentaherramientas.domain.model.valueobject;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

import java.util.regex.Pattern;

/**
 * Value Object de correo electrónico.
 * Se valida al crearse y se guarda en minúsculas: "Juan@Mail.com" y "juan@mail.com" son el mismo email.
 */
public record Email(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    public Email {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException("El email es obligatorio");
        }
        valor = valor.trim().toLowerCase();
        if (!FORMATO.matcher(valor).matches()) {
            throw new ReglaDeNegocioException("El email '" + valor + "' no tiene un formato válido");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
