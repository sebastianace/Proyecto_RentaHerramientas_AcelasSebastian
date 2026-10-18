package com.rentaherramientas.application.dtos;

/** Archivo listo para descargar (ej. el PDF de una factura). */
public record ArchivoGenerado(String nombre, String tipoContenido, byte[] contenido) {
}
