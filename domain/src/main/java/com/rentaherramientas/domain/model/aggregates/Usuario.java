package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cuenta de acceso a la plataforma.
 *
 * <p>Guarda la clave ya cifrada (hash): el dominio nunca ve la clave en texto plano.
 * Los cambios de estado se hacen con métodos con nombre de negocio
 * ({@link #desactivar()}, {@link #actualizarDatosPersonales}) en lugar de setters,
 * para que las reglas no se puedan saltar.</p>
 */
public class Usuario {

    private final Long id;
    private final Email email;
    private String passwordHash;
    private String nombres;
    private String apellidos;
    private String telefono;
    private boolean activo;
    private final Set<RolNombre> roles;

    private Usuario(Long id, Email email, String passwordHash, String nombres, String apellidos,
                    String telefono, boolean activo, Set<RolNombre> roles) {
        if (email == null) {
            throw new ReglaDeNegocioException("El email es obligatorio");
        }
        if (roles == null || roles.isEmpty()) {
            throw new ReglaDeNegocioException("El usuario debe tener al menos un rol");
        }
        this.id = id;
        this.email = email;
        this.passwordHash = ReglaDeNegocioException.requerido(passwordHash, "password");
        this.nombres = ReglaDeNegocioException.requerido(nombres, "nombres");
        this.apellidos = ReglaDeNegocioException.requerido(apellidos, "apellidos");
        this.telefono = ReglaDeNegocioException.opcional(telefono);
        this.activo = activo;
        this.roles = EnumSet.copyOf(roles);
    }

    /** Crea un usuario nuevo (aún sin id), activo y con un solo rol. */
    public static Usuario nuevo(Email email, String passwordHash, String nombres, String apellidos,
                                String telefono, RolNombre rol) {
        return new Usuario(null, email, passwordHash, nombres, apellidos, telefono, true, Set.of(rol));
    }

    /** Reconstruye un usuario existente a partir de lo guardado en la base de datos. */
    public static Usuario reconstruir(Long id, Email email, String passwordHash, String nombres, String apellidos,
                                      String telefono, boolean activo, Set<RolNombre> roles) {
        return new Usuario(id, email, passwordHash, nombres, apellidos, telefono, activo, roles);
    }

    public void actualizarDatosPersonales(String nombres, String apellidos, String telefono) {
        this.nombres = ReglaDeNegocioException.requerido(nombres, "nombres");
        this.apellidos = ReglaDeNegocioException.requerido(apellidos, "apellidos");
        this.telefono = ReglaDeNegocioException.opcional(telefono);
    }

    /** Borrado lógico: el usuario conserva su historial pero ya no puede iniciar sesión. */
    /** Cambia la contraseña (recibe el hash ya cifrado: el dominio nunca ve la clave en texto plano). */
    public void cambiarPassword(String nuevoPasswordHash) {
        this.passwordHash = ReglaDeNegocioException.requerido(nuevoPasswordHash, "password");
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public boolean tieneRol(RolNombre rol) {
        return roles.contains(rol);
    }

    public Long getId() { return id; }
    public Email getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
    public Set<RolNombre> getRoles() { return Set.copyOf(roles); }
}
