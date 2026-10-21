package com.rentaherramientas.application.ports.in;

/** Recuperación de la contraseña olvidada en dos pasos: pedir el enlace y crear la clave nueva. */
public interface RecuperacionPasswordUseCase {

    /**
     * Envía un enlace de recuperación si el correo pertenece a un usuario activo.
     * Si no existe, no hace nada y no lo revela (así nadie puede averiguar qué correos están registrados).
     */
    void solicitarRecuperacion(String email);

    /** Cambia la contraseña usando el token del enlace. El token vence y solo sirve una vez. */
    void restablecerPassword(String token, String nuevaPassword);
}
