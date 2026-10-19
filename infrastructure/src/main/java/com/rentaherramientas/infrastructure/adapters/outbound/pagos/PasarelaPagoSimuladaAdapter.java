package com.rentaherramientas.infrastructure.adapters.outbound.pagos;

import com.rentaherramientas.application.dtos.ResultadoCobro;
import com.rentaherramientas.application.dtos.SolicitudCobro;
import com.rentaherramientas.application.ports.out.PasarelaPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Pasarela de pagos SIMULADA para desarrollo y pruebas. No mueve dinero real.
 *
 * <p>Reglas de la simulación (como las tarjetas de prueba de Wompi o Stripe):</p>
 * <ul>
 *   <li>Tarjeta terminada en <b>0000</b>: rechazada por fondos insuficientes.</li>
 *   <li>Tarjeta terminada en <b>1111</b>: rechazada por el banco emisor.</li>
 *   <li>Cualquier otra tarjeta, PSE o Nequi: aprobado.</li>
 * </ul>
 *
 * <p>Para cobrar de verdad se crea otro adaptador (ej. WompiPasarelaAdapter) que implemente
 * el mismo puerto. Los casos de uso no cambian.</p>
 */
@Component
public class PasarelaPagoSimuladaAdapter implements PasarelaPagoPort {

    private static final Logger log = LoggerFactory.getLogger(PasarelaPagoSimuladaAdapter.class);
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        byte[] bytes = new byte[5];
        aleatorio.nextBytes(bytes);
        String referencia = "TX-" + HexFormat.of().withUpperCase().formatHex(bytes);
        log.info("Cobro simulado: {} -> referencia {}", solicitud, referencia);

        if (solicitud.metodo().esTarjeta()) {
            String numero = solicitud.numeroTarjeta().replace(" ", "");
            if (numero.endsWith("0000")) {
                return new ResultadoCobro(false, referencia, "Pago rechazado: fondos insuficientes");
            }
            if (numero.endsWith("1111")) {
                return new ResultadoCobro(false, referencia, "Pago rechazado por el banco emisor");
            }
        }
        return new ResultadoCobro(true, referencia, "Pago aprobado");
    }
}
