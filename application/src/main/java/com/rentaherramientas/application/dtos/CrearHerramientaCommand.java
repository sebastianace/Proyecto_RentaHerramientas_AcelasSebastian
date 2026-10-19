package com.rentaherramientas.application.dtos;

import java.math.BigDecimal;

/**
 * @param proveedorId solo lo usa el ADMIN (para crear en nombre de un proveedor);
 *                    si quien crea es un PROVEEDOR se ignora y se usa el suyo
 */
public record CrearHerramientaCommand(
        Long proveedorId,
        Long categoriaId,
        String nombre,
        String descripcion,
        String marca,
        String modelo,
        BigDecimal precioPorDia,
        int stockTotal) {
}
