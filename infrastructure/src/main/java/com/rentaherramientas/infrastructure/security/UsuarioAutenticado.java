package com.rentaherramientas.infrastructure.security;

import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuario que viene dentro del token. Se guarda en el SecurityContext durante la petición
 * y los controladores lo reciben con {@code @AuthenticationPrincipal}.
 */
public record UsuarioAutenticado(Long id, String email, Set<RolNombre> roles) {

    /** Spring Security espera los roles con el prefijo "ROLE_" para que funcione hasRole('ADMIN'). */
    public List<GrantedAuthority> authorities() {
        return roles.stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .toList();
    }

    public Solicitante aSolicitante() {
        return new Solicitante(id, roles);
    }

    static Set<RolNombre> rolesDesde(List<?> valores) {
        return valores.stream().map(String::valueOf).map(RolNombre::valueOf).collect(Collectors.toSet());
    }
}
