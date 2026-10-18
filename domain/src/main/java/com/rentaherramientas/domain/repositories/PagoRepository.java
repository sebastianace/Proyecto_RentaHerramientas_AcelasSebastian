package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Pago;

public interface PagoRepository {

    Pago guardar(Pago pago);
}
