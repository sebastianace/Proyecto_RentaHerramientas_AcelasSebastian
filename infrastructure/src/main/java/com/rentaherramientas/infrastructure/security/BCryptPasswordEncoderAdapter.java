package com.rentaherramientas.infrastructure.security;

import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adaptador del puerto PasswordEncoderPort con el PasswordEncoder de Spring (BCrypt). */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordEncoderAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String cifrar(String passwordPlano) {
        return passwordEncoder.encode(passwordPlano);
    }
}
