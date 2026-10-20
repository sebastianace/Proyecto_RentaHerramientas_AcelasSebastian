package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

/**
 * Reglas de formato compartidas por los DTOs de entrada (Bean Validation).
 *
 * <p>Se validan en la frontera HTTP para responder 400 con el error de cada campo antes de llegar al caso de uso.
 * {@code \p{L}} significa "cualquier letra", incluidas las tildes y la ñ.</p>
 */
final class ValidacionesComunes {

    /** 8 a 72 caracteres (72 es el límite de BCrypt), al menos una letra y un número. */
    static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    static final String MENSAJE_PASSWORD = "La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un número";

    /** Colombia: 10 dígitos. Celular 3XXXXXXXXX o fijo 60XXXXXXXX. Vacío se permite porque el teléfono es opcional. */
    static final String REGEX_TELEFONO = "^$|^(3\\d{9}|60\\d{8})$";
    static final String MENSAJE_TELEFONO = "El teléfono debe tener 10 dígitos, solo números (celular 3XXXXXXXXX o fijo 60XXXXXXXX)";

    /** Nombres y apellidos: solo letras, espacios, punto, apóstrofo o guion. Nunca números. */
    static final String REGEX_NOMBRE = "^(?=.*\\p{L})[\\p{L} .'-]{2,80}$";
    static final String MENSAJE_NOMBRES = "Los nombres solo pueden tener letras (sin números ni símbolos)";
    static final String MENSAJE_APELLIDOS = "Los apellidos solo pueden tener letras (sin números ni símbolos)";

    /** Ciudad: solo letras, espacios, punto, apóstrofo o guion (ej. "Bogotá D.C."). */
    static final String REGEX_CIUDAD = "^(?=.*\\p{L})[\\p{L} .'-]{2,80}$";
    static final String MENSAJE_CIUDAD = "La ciudad solo puede tener letras (sin números ni símbolos)";

    /** Razón social: letras, números y los signos habituales de una empresa (ej. "Equipos S.A.S.", "A&B Ltda."). */
    static final String REGEX_RAZON_SOCIAL = "^(?=.*\\p{L})[\\p{L}\\d .,&'-]{3,150}$";
    static final String MENSAJE_RAZON_SOCIAL = "La razón social solo puede tener letras, números, espacios y . , & ' -";

    /** Categoría: solo letras, espacios, comas y guiones (ej. "Taladros y Rotomartillos"). */
    static final String REGEX_CATEGORIA = "^(?=.*\\p{L})[\\p{L} ,-]{3,80}$";
    static final String MENSAJE_CATEGORIA = "El nombre de la categoría solo puede tener letras (sin números ni símbolos)";

    static final String MENSAJE_DOCUMENTO = "Número de documento inválido: la cédula (CC/CE) solo admite números (6 a 10), "
            + "el NIT números con dígito de verificación opcional (900123456-7) y el pasaporte letras y números (6 a 12)";

    private ValidacionesComunes() {
    }

    /** El formato del documento depende de su tipo. Si falta el tipo, lo reporta su propia validación (@NotNull). */
    static boolean documentoValido(TipoDocumento tipo, String numero) {
        if (tipo == null || numero == null || numero.isBlank()) {
            return true;
        }
        String n = numero.trim();
        return switch (tipo) {
            case CC, CE -> n.matches("\\d{6,10}");
            case NIT -> n.matches("\\d{9,10}(-\\d)?");
            case PASAPORTE -> n.matches("[A-Za-z0-9]{6,12}");
        };
    }
}
