package com.rentaherramientas.infrastructure.security;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories.UsuarioJpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le dice a Spring Security cómo cargar un usuario por su email para verificar la clave en el login.
 * Spring usa este bean + el PasswordEncoder (BCrypt) para armar el AuthenticationManager.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UserDetailsServiceImpl(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        UsuarioEntity usuario = usuarioJpaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRoles().stream().map(rol -> rol.getNombre().name()).toArray(String[]::new))
                .disabled(!usuario.isActivo())
                .build();
    }
}
