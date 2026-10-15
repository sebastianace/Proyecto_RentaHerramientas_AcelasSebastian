package com.rentaherramientas.infrastructure.config;

import com.rentaherramientas.application.ports.in.AutenticacionUseCase;
import com.rentaherramientas.application.ports.in.GestionCategoriasUseCase;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.application.ports.in.GestionHerramientasUseCase;
import com.rentaherramientas.application.ports.in.GestionProveedoresUseCase;
import com.rentaherramientas.application.ports.out.AlmacenamientoArchivosPort;
import com.rentaherramientas.application.ports.out.AutenticadorPort;
import com.rentaherramientas.application.ports.out.ConsultaHerramientasPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.application.ports.out.TokenProviderPort;
import com.rentaherramientas.application.usecases.AutenticacionService;
import com.rentaherramientas.application.usecases.GestionCategoriasService;
import com.rentaherramientas.application.usecases.GestionClientesService;
import com.rentaherramientas.application.usecases.GestionHerramientasService;
import com.rentaherramientas.application.usecases.GestionProveedoresService;
import com.rentaherramientas.domain.repositories.CategoriaRepository;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Registra los casos de uso como beans de Spring.
 *
 * <p>Las clases de la capa de aplicación son Java puro (sin @Service ni @Transactional),
 * así que aquí se crean con "new" y se envuelven en un <b>proxy transaccional</b>: cada
 * método del caso de uso se ejecuta dentro de una transacción. Si algo falla a mitad de
 * camino (ej. se guarda el usuario pero falla el cliente) se hace rollback de todo.</p>
 *
 * <p>Métodos que empiezan por obtener* o listar* usan transacciones de solo lectura;
 * el resto, transacciones de lectura y escritura.</p>
 */
@Configuration
public class UseCaseConfig {

    private final TransactionInterceptor transacciones;

    public UseCaseConfig(PlatformTransactionManager transactionManager) {
        DefaultTransactionAttribute escritura = new DefaultTransactionAttribute();
        DefaultTransactionAttribute lectura = new DefaultTransactionAttribute();
        lectura.setReadOnly(true);

        NameMatchTransactionAttributeSource reglas = new NameMatchTransactionAttributeSource();
        reglas.addTransactionalMethod("obtener*", lectura);
        reglas.addTransactionalMethod("listar*", lectura);
        reglas.addTransactionalMethod("*", escritura);

        this.transacciones = new TransactionInterceptor(transactionManager, reglas);
    }

    @Bean
    public GestionClientesUseCase gestionClientesUseCase(ClienteRepository clienteRepository,
                                                         UsuarioRepository usuarioRepository,
                                                         PasswordEncoderPort passwordEncoder) {
        return transaccional(new GestionClientesService(clienteRepository, usuarioRepository, passwordEncoder),
                GestionClientesUseCase.class);
    }

    @Bean
    public GestionProveedoresUseCase gestionProveedoresUseCase(ProveedorRepository proveedorRepository,
                                                               UsuarioRepository usuarioRepository,
                                                               PasswordEncoderPort passwordEncoder) {
        return transaccional(new GestionProveedoresService(proveedorRepository, usuarioRepository, passwordEncoder),
                GestionProveedoresUseCase.class);
    }

    @Bean
    public AutenticacionUseCase autenticacionUseCase(AutenticadorPort autenticador, TokenProviderPort tokenProvider,
                                                     UsuarioRepository usuarioRepository,
                                                     ClienteRepository clienteRepository,
                                                     ProveedorRepository proveedorRepository,
                                                     GestionClientesUseCase gestionClientes) {
        return transaccional(new AutenticacionService(autenticador, tokenProvider, usuarioRepository,
                clienteRepository, proveedorRepository, gestionClientes), AutenticacionUseCase.class);
    }

    @Bean
    public GestionCategoriasUseCase gestionCategoriasUseCase(CategoriaRepository categoriaRepository) {
        return transaccional(new GestionCategoriasService(categoriaRepository), GestionCategoriasUseCase.class);
    }

    @Bean
    public GestionHerramientasUseCase gestionHerramientasUseCase(HerramientaRepository herramientaRepository,
                                                                 CategoriaRepository categoriaRepository,
                                                                 ProveedorRepository proveedorRepository,
                                                                 ConsultaHerramientasPort consultaHerramientas,
                                                                 AlmacenamientoArchivosPort almacenamiento) {
        return transaccional(new GestionHerramientasService(herramientaRepository, categoriaRepository,
                proveedorRepository, consultaHerramientas, almacenamiento), GestionHerramientasUseCase.class);
    }

    /** Envuelve el caso de uso en un proxy que abre y cierra la transacción en cada llamada. */
    private <T> T transaccional(T casoDeUso, Class<T> puerto) {
        ProxyFactory fabrica = new ProxyFactory();
        fabrica.setTarget(casoDeUso);
        fabrica.setInterfaces(puerto);
        fabrica.addAdvice(transacciones);
        return puerto.cast(fabrica.getProxy());
    }
}
