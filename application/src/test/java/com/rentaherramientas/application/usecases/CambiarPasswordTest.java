package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.exceptions.CredencialesInvalidasException;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.application.ports.out.AutenticadorPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.application.ports.out.TokenProviderPort;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un usuario autenticado cambia su propia contraseña. */
@ExtendWith(MockitoExtension.class)
class CambiarPasswordTest {

    @Mock AutenticadorPort autenticador;
    @Mock TokenProviderPort tokenProvider;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock GestionClientesUseCase gestionClientes;
    @Mock PasswordEncoderPort passwordEncoder;

    AutenticacionService servicio;
    final Usuario usuario = Usuario.reconstruir(23L, new Email("juanpablo.ardila@gmail.com"), "$2a$hashViejo",
            "Juan Pablo", "Ardila", "3158735513", true, Set.of(RolNombre.CLIENTE));

    @BeforeEach
    void setUp() {
        servicio = new AutenticacionService(autenticador, tokenProvider, usuarioRepository, clienteRepository,
                proveedorRepository, gestionClientes, passwordEncoder);
        when(usuarioRepository.buscarPorId(23L)).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Con la contraseña actual correcta, guarda la nueva cifrada")
    void cambia() {
        when(passwordEncoder.cifrar("NuevaClave2026")).thenReturn("$2a$hashNuevo");

        servicio.cambiarPassword(23L, "Renta2026*", "NuevaClave2026");

        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$hashNuevo");
        verify(usuarioRepository).guardar(usuario);
    }

    @Test
    @DisplayName("Si la contraseña actual es incorrecta responde 422 (regla de negocio), no 401, y no cambia nada")
    void actualIncorrecta() {
        doThrow(new CredencialesInvalidasException()).when(autenticador).autenticar("juanpablo.ardila@gmail.com", "mala");

        assertThatThrownBy(() -> servicio.cambiarPassword(23L, "mala", "NuevaClave2026"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("actual no es correcta");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("La contraseña nueva no puede ser igual a la actual")
    void igualALaActual() {
        assertThatThrownBy(() -> servicio.cambiarPassword(23L, "Renta2026*", "Renta2026*"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("distinta");
        verify(usuarioRepository, never()).guardar(any());
    }
}
