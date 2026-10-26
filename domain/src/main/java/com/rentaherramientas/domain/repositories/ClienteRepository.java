package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

import java.util.Optional;

/** Puerto de salida para persistir clientes (junto con su usuario). */
public interface ClienteRepository {

    /** Crea o actualiza el cliente y su usuario en una sola operación. Devuelve el cliente con su id. */
    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(Long id);

    Optional<Cliente> buscarPorUsuarioId(Long usuarioId);

    boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    /**
     * Lista los clientes paginados. {@code texto} busca en nombres, apellidos, correo y número de documento;
     * {@code ciudad} filtra por ciudad. Ambos son opcionales (null o vacío = sin filtro).
     */
    Pagina<Cliente> listar(String texto, String ciudad, SolicitudPagina solicitud);
}
