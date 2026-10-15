package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Proveedor;

import java.util.Optional;

/** Puerto de salida para persistir proveedores (junto con su usuario). */
public interface ProveedorRepository {

    Proveedor guardar(Proveedor proveedor);

    Optional<Proveedor> buscarPorId(Long id);

    Optional<Proveedor> buscarPorUsuarioId(Long usuarioId);

    boolean existePorNit(String nit);

    Pagina<Proveedor> listar(SolicitudPagina solicitud);
}
