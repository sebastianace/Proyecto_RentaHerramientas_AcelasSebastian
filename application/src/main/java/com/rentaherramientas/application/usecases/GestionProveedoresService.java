package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ActualizarProveedorCommand;
import com.rentaherramientas.application.dtos.ProveedorResult;
import com.rentaherramientas.application.dtos.RegistrarProveedorCommand;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.mappers.IdentidadMapper;
import com.rentaherramientas.application.ports.in.GestionProveedoresUseCase;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.domain.repositories.UsuarioRepository;

/**
 * Casos de uso del CRUD de proveedores.
 * Solo el ADMIN registra proveedores; cada proveedor puede consultar y editar su propio perfil.
 */
public class GestionProveedoresService implements GestionProveedoresUseCase {

    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoder;

    public GestionProveedoresService(ProveedorRepository proveedorRepository, UsuarioRepository usuarioRepository,
                                     PasswordEncoderPort passwordEncoder) {
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ProveedorResult crear(RegistrarProveedorCommand cmd) {
        Email email = new Email(cmd.email());
        if (usuarioRepository.existePorEmail(email)) {
            throw new ConflictoException("Ya existe un usuario registrado con el email " + email);
        }
        if (proveedorRepository.existePorNit(cmd.nit())) {
            throw new ConflictoException("Ya existe un proveedor con el NIT " + cmd.nit());
        }
        Usuario usuario = Usuario.nuevo(email, passwordEncoder.cifrar(cmd.password()), cmd.nombres(),
                cmd.apellidos(), cmd.telefono(), RolNombre.PROVEEDOR);
        Proveedor proveedor = Proveedor.nuevo(usuario, cmd.razonSocial(), cmd.nit(), cmd.direccion(),
                cmd.ciudad(), cmd.descripcion());
        return IdentidadMapper.aResult(proveedorRepository.guardar(proveedor));
    }

    @Override
    public ProveedorResult obtener(Long id, Solicitante solicitante) {
        Proveedor proveedor = buscar(id);
        validarAcceso(proveedor, solicitante);
        return IdentidadMapper.aResult(proveedor);
    }

    @Override
    public Pagina<ProveedorResult> listar(SolicitudPagina solicitud) {
        return proveedorRepository.listar(solicitud).map(IdentidadMapper::aResult);
    }

    @Override
    public ProveedorResult actualizar(Long id, ActualizarProveedorCommand cmd, Solicitante solicitante) {
        Proveedor proveedor = buscar(id);
        validarAcceso(proveedor, solicitante);
        proveedor.actualizar(cmd.nombres(), cmd.apellidos(), cmd.telefono(), cmd.razonSocial(),
                cmd.direccion(), cmd.ciudad(), cmd.descripcion());
        return IdentidadMapper.aResult(proveedorRepository.guardar(proveedor));
    }

    @Override
    public void desactivar(Long id) {
        Proveedor proveedor = buscar(id);
        proveedor.desactivar();
        proveedorRepository.guardar(proveedor);
    }

    @Override
    public void activar(Long id) {
        Proveedor proveedor = buscar(id);
        proveedor.activar();
        proveedorRepository.guardar(proveedor);
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", id));
    }

    private void validarAcceso(Proveedor proveedor, Solicitante solicitante) {
        if (!solicitante.esAdmin() && !proveedor.perteneceA(solicitante.usuarioId())) {
            throw new AccesoDenegadoException("Solo puedes consultar o modificar tu propio perfil de proveedor");
        }
    }
}
