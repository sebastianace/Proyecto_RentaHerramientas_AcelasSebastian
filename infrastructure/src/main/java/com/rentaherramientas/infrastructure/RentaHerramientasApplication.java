package com.rentaherramientas.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la Plataforma de Renta de Herramientas.
 *
 * <p>La clase principal vive en el módulo {@code infrastructure} porque es el único
 * módulo que conoce Spring. {@code scanBasePackages} amplía el escaneo a todo
 * {@code com.rentaherramientas} para que Spring encuentre los beans de los demás módulos.
 * Tomcat viene embebido dentro de este mismo ejecutable.</p>
 */
@SpringBootApplication(scanBasePackages = "com.rentaherramientas")
public class RentaHerramientasApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentaHerramientasApplication.class, args);
    }
}
