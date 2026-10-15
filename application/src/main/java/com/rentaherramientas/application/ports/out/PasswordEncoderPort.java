package com.rentaherramientas.application.ports.out;

/** Cifra claves. La aplicación no sabe qué algoritmo se usa (en infraestructura es BCrypt). */
public interface PasswordEncoderPort {

    String cifrar(String passwordPlano);
}
