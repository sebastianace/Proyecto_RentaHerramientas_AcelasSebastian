package com.rentaherramientas.application.ports.out;

import java.time.Duration;

/**
 * Puerto de salida para enviar correos. El caso de uso no sabe CÓMO se envía: en desarrollo lo implementa un
 * adaptador que escribe el correo en la consola del servidor; en producción se conectaría un adaptador SMTP
 * (Gmail, SendGrid…) sin cambiar ningún caso de uso.
 */
public interface EnvioCorreoPort {

    void enviarRecuperacionPassword(String email, String nombre, String enlace, Duration vigencia);
}
