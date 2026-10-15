package com.rentaherramientas.infrastructure.adapters.outbound.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades media.storage.*: carpeta del disco donde se guardan las imágenes. */
@ConfigurationProperties(prefix = "media.storage")
public record MediaProperties(String basePath) {

    /** Prefijo de las URLs públicas de los archivos guardados. */
    public static final String URL_PUBLICA = "/media/";
}
