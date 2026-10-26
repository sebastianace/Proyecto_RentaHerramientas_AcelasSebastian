package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.AutenticacionResult;
import com.rentaherramientas.application.dtos.ClienteResult;
import com.rentaherramientas.application.dtos.LoginCommand;
import com.rentaherramientas.application.dtos.PerfilResult;
import com.rentaherramientas.application.dtos.RegistrarClienteCommand;
import com.rentaherramientas.application.dtos.TokenGenerado;
import com.rentaherramientas.application.exceptions.CredencialesInvalidasException;
import com.rentaherramientas.application.mappers.IdentidadMapper;
import com.rentaherramientas.application.ports.in.AutenticacionUseCase;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.application.ports.out.AutenticadorPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.application.ports.out.TokenProviderPort;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;

/**
 * Inicio de sesión y registro.
 *
 * <p>Flujo del login: (1) el autenticador verifica email y clave, (2) se carga el usuario,
 * (3) se genera el token. Ninguno de esos pasos conoce Spring ni JWT: todo pasa por puertos.</p>
 */
public class AutenticacionService implements AutenticacionUseCase {

    private static final String TIPO_TOKEN = "Bearer";

    private final AutenticadorPort autenticador;
    private final TokenProviderPort tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final GestionClientesUseCase gestionClientes;
    private final PasswordEncoderPort passwordEncoder;

    public AutenticacionService(AutenticadorPort autenticador, TokenProviderPort tokenProvider,
                                UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                                ProveedorRepository proveedorRepository, GestionClientesUseCase gestionClientes,
                                PasswordEncoderPort passwordEncoder) {
        this.autenticador = autenticador;
        this.tokenProvider = tokenProvider;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.gestionClientes = gestionClientes;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AutenticacionResult iniciarSesion(LoginCommand cmd) {
        Email email = new Email(cmd.email());
        autenticador.autenticar(email.valor(), cmd.password());
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);
        return emitirToken(usuario);
    }

    @Override
    public AutenticacionResult registrarCliente(RegistrarClienteCommand cmd) {
        ClienteResult cliente = gestionClientes.crear(cmd);
        Usuario usuario = usuarioRepository.buscarPorId(cliente.usuario().id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", cliente.usuario().id()));
        return emitirToken(usuario);
    }

    @Override
    public PerfilResult obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
        Long clienteId = clienteRepository.buscarPorUsuarioId(usuarioId).map(Cliente::getId).orElse(null);
        Long proveedorId = proveedorRepository.buscarPorUsuarioId(usuarioId).map(Proveedor::getId).orElse(null);
        return new PerfilResult(IdentidadMapper.aResult(usuario), clienteId, proveedorId);
    }

    @Override
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
        if (passwordActual != null && passwordActual.equals(passwordNueva)) {
            throw new ReglaDeNegocioException("La contraseña nueva debe ser distinta de la actual");
        }
        try {
            autenticador.autenticar(usuario.getEmail().valor(), passwordActual);
        } catch (CredencialesInvalidasException e) {
            // 422 y no 401: un 401 con token significa "sesión vencida" y el frontend cerraría la sesión
            throw new ReglaDeNegocioException("La contraseña actual no es correcta");
        }
        usuario.cambiarPassword(passwordEncoder.cifrar(passwordNueva));
        usuarioRepository.guardar(usuario);
    }

    private AutenticacionResult emitirToken(Usuario usuario) {
        TokenGenerado token = tokenProvider.generar(usuario);
        return new AutenticacionResult(token.token(), TIPO_TOKEN, token.expiraEn(), IdentidadMapper.aResult(usuario));
    }
}
