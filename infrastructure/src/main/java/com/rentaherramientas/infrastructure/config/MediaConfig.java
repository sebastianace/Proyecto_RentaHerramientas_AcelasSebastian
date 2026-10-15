package com.rentaherramientas.infrastructure.config;

import com.rentaherramientas.infrastructure.adapters.outbound.storage.MediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Publica la carpeta de imágenes: una petición a /media/herramientas/5/foto.jpg
 * devuelve el archivo {media.storage.base-path}/herramientas/5/foto.jpg.
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaConfig implements WebMvcConfigurer {

    private final MediaProperties propiedades;

    public MediaConfig(MediaProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Paths.get(propiedades.basePath()).toAbsolutePath().normalize().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler(MediaProperties.URL_PUBLICA + "**").addResourceLocations(ubicacion);
    }
}
