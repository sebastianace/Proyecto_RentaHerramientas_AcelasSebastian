package com.rentaherramientas.application.ports.out;

import java.io.InputStream;

/** Guarda y borra archivos. En infraestructura se implementa con el disco local; mañana podría ser S3. */
public interface AlmacenamientoArchivosPort {

    /**
     * @param carpeta   subcarpeta lógica, ej. "herramientas/15"
     * @param extension extensión del archivo sin punto, ej. "jpg"
     * @return URL pública con la que se puede descargar el archivo
     */
    String guardar(String carpeta, String extension, InputStream contenido);

    /** Borra el archivo si fue guardado por este almacenamiento (URLs externas se ignoran). */
    void eliminar(String url);
}
