package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ActualizarClienteCommand;
import com.rentaherramientas.application.dtos.ClienteResult;
import com.rentaherramientas.application.dtos.RegistrarClienteCommand;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.mappers.IdentidadMapper;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.domain.repositories.UsuarioRepository;

/**
 * Casos de uso del CRUD de clientes.
 * Las reglas de acceso son: el ADMIN ve y edita a todos; un CLIENTE solo a sí mismo.
 */
public class GestionClientesService implements GestionClientesUseCase {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoder;

    public GestionClientesService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                                  PasswordEncoderPort passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ClienteResult crear(RegistrarClienteCommand cmd) {
        Email email = new Email(cmd.email());
        if (usuarioRepository.existePorEmail(email)) {
            throw new ConflictoException("Ya existe un usuario registrado con el email " + email);
        }
        if (clienteRepository.existePorDocumento(cmd.tipoDocumento(), cmd.numeroDocumento())) {
            throw new ConflictoException("Ya existe un cliente con el documento " + cmd.tipoDocumento()
                    + " " + cmd.numeroDocumento());
        }
        Usuario usuario = Usuario.nuevo(email, passwordEncoder.cifrar(cmd.password()), cmd.nombres(),
                cmd.apellidos(), cmd.telefono(), RolNombre.CLIENTE);
        Cliente cliente = Cliente.nuevo(usuario, cmd.tipoDocumento(), cmd.numeroDocumento(),
                cmd.direccion(), cmd.ciudad());
        return IdentidadMapper.aResult(clienteRepository.guardar(cliente));
    }

    @Override
    public ClienteResult obtener(Long id, Solicitante solicitante) {
        Cliente cliente = buscar(id);
        validarAcceso(cliente, solicitante);
        return IdentidadMapper.aResult(cliente);
    }

    @Override
    public Pagina<ClienteResult> listar(String texto, String ciudad, SolicitudPagina solicitud) {
        // Los textos vacíos se tratan como "sin filtro" y se recortan los espacios sobrantes
        return clienteRepository.listar(normalizar(texto), normalizar(ciudad), solicitud)
                .map(IdentidadMapper::aResult);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @Override
    public ClienteResult actualizar(Long id, ActualizarClienteCommand cmd, Solicitante solicitante) {
        Cliente cliente = buscar(id);
        validarAcceso(cliente, solicitante);
        cliente.actualizar(cmd.nombres(), cmd.apellidos(), cmd.telefono(), cmd.direccion(), cmd.ciudad());
        return IdentidadMapper.aResult(clienteRepository.guardar(cliente));
    }

    @Override
    public void desactivar(Long id) {
        Cliente cliente = buscar(id);
        cliente.desactivar();
        clienteRepository.guardar(cliente);
    }

    @Override
    public void activar(Long id) {
        Cliente cliente = buscar(id);
        cliente.activar();
        clienteRepository.guardar(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private void validarAcceso(Cliente cliente, Solicitante solicitante) {
        if (!solicitante.esAdmin() && !cliente.perteneceA(solicitante.usuarioId())) {
            throw new AccesoDenegadoException("Solo puedes consultar o modificar tu propio perfil de cliente");
        }
    }
}
