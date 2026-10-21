package com.rentaherramientas.infrastructure.adapters.outbound.correo;

import com.rentaherramientas.application.ports.out.EnvioCorreoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adaptador de correo para desarrollo y demostración: en vez de enviar un email real, escribe el mensaje en la
 * consola del servidor (igual que la pasarela de pagos simulada).
 *
 * <p>Para enviar correos reales se crea otro adaptador de {@link EnvioCorreoPort} con SMTP (por ejemplo con
 * JavaMailSender) y ningún caso de uso cambia: esa es la ventaja de depender del puerto y no de esta clase.</p>
 */
@Component
public class CorreoConsolaAdapter implements EnvioCorreoPort {

    private static final Logger log = LoggerFactory.getLogger(CorreoConsolaAdapter.class);

    @Override
    public void enviarRecuperacionPassword(String email, String nombre, String enlace, Duration vigencia) {
        log.info("""

                ============================== CORREO (simulado) ==============================
                Para:    {}
                Asunto:  Recupera tu contraseña de ObraYa

                Hola {}, recibimos una solicitud para cambiar tu contraseña.
                Abre este enlace para crear una nueva (vence en {} minutos y solo sirve una vez):

                {}

                Si no fuiste tú, ignora este mensaje: tu contraseña actual sigue funcionando.
                ===============================================================================
                """, email, nombre, vigencia.toMinutes(), enlace);
    }
}
