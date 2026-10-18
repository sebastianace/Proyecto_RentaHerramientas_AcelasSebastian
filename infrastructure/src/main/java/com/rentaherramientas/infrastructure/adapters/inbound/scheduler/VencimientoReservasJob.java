package com.rentaherramientas.infrastructure.adapters.inbound.scheduler;

import com.rentaherramientas.application.ports.in.GestionReservasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada cierto tiempo revisa las reservas APROBADAS que no se pagaron antes de su fecha límite
 * y las pasa a VENCIDA, liberando las unidades para otros clientes.
 *
 * <p>Es un adaptador de entrada igual que un controlador: en vez de una petición HTTP,
 * lo dispara el reloj. La lógica sigue estando en el caso de uso.</p>
 */
@Component
public class VencimientoReservasJob {

    private static final Logger log = LoggerFactory.getLogger(VencimientoReservasJob.class);

    private final GestionReservasUseCase gestionReservas;

    public VencimientoReservasJob(GestionReservasUseCase gestionReservas) {
        this.gestionReservas = gestionReservas;
    }

    @Scheduled(initialDelayString = "${app.reservas.revision-inicial:PT20S}",
               fixedDelayString = "${app.reservas.revision-cada:PT5M}")
    public void vencerReservasSinPago() {
        int vencidas = gestionReservas.vencerReservasSinPago();
        if (vencidas > 0) {
            log.info("Se marcaron {} reserva(s) como VENCIDA por falta de pago", vencidas);
        }
    }
}
