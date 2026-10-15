package com.rentaherramientas.infrastructure.adapters.outbound.storage;

import com.rentaherramientas.application.ports.out.AlmacenamientoArchivosPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda los archivos en el disco local, dentro de media.storage.base-path.
 *
 * <p>Seguridad: el nombre del archivo lo genera el servidor (UUID) y se verifica que la ruta final
 * quede dentro de la carpeta base, para evitar ataques de "path traversal" (ej. ../../windows).</p>
 */
@Component
public class AlmacenamientoLocalAdapter implements AlmacenamientoArchivosPort {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoLocalAdapter.class);

    private final Path raiz;

    public AlmacenamientoLocalAdapter(MediaProperties propiedades) {
        this.raiz = Paths.get(propiedades.basePath()).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String extension, InputStream contenido) {
        Path directorio = dentroDeRaiz(raiz.resolve(carpeta));
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream entrada = contenido) {
            Files.createDirectories(directorio);
            Files.copy(entrada, directorio.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return MediaProperties.URL_PUBLICA + carpeta + "/" + nombre;
    }

    @Override
    public void eliminar(String url) {
        if (url == null || !url.startsWith(MediaProperties.URL_PUBLICA)) {
            return; // URL externa (ej. imágenes de ejemplo): no es un archivo nuestro
        }
        Path archivo = dentroDeRaiz(raiz.resolve(url.substring(MediaProperties.URL_PUBLICA.length())));
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo {}: {}", archivo, e.getMessage());
        }
    }

    private Path dentroDeRaiz(Path ruta) {
        Path normalizada = ruta.normalize();
        if (!normalizada.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo no permitida");
        }
        return normalizada;
    }
}
