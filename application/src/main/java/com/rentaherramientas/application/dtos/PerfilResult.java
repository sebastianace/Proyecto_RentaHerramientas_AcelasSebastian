package com.rentaherramientas.application.dtos;

/**
 * Perfil del usuario autenticado. Según su rol, trae el id de cliente o de proveedor
 * (el otro queda en null). El frontend lo usa para armar el panel de cada rol.
 */
public record PerfilResult(UsuarioResult usuario, Long clienteId, Long proveedorId) {
}
