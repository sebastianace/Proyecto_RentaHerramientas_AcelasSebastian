package com.rentaherramientas.application.dtos;

import java.io.InputStream;

/**
 * Archivo de imagen recibido. Se usa InputStream (Java estándar) y no MultipartFile (Spring)
 * para que la capa de aplicación no dependa del framework web.
 */
public record SubirImagenCommand(Long herramientaId, String contentType, long tamanio, InputStream contenido,
                                 boolean principal) {
}
