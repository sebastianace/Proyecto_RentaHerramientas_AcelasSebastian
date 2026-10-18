package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.ResultadoCobro;
import com.rentaherramientas.application.dtos.SolicitudCobro;

/**
 * Pasarela de pagos. En este proyecto se implementa con un adaptador SIMULADO;
 * para cobrar de verdad (Wompi, PayU, Mercado Pago) solo habría que escribir otro adaptador.
 */
public interface PasarelaPagoPort {

    ResultadoCobro cobrar(SolicitudCobro solicitud);
}
