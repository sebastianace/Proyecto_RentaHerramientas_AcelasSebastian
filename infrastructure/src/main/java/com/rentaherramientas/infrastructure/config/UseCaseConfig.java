package com.rentaherramientas.infrastructure.config;

import com.rentaherramientas.application.config.PoliticaRecuperacion;
import com.rentaherramientas.application.config.PoliticaReservas;
import com.rentaherramientas.application.ports.in.AutenticacionUseCase;
import com.rentaherramientas.application.ports.in.GestionCategoriasUseCase;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.application.ports.in.GestionDevolucionesUseCase;
import com.rentaherramientas.application.ports.in.GestionHerramientasUseCase;
import com.rentaherramientas.application.ports.in.GestionPagosUseCase;
import com.rentaherramientas.application.ports.in.GestionProveedoresUseCase;
import com.rentaherramientas.application.ports.in.GestionReservasUseCase;
import com.rentaherramientas.application.ports.in.RecuperacionPasswordUseCase;
import com.rentaherramientas.application.ports.out.EnvioCorreoPort;
import com.rentaherramientas.application.usecases.RecuperacionPasswordService;
import com.rentaherramientas.domain.repositories.TokenRecuperacionRepository;
import com.rentaherramientas.application.ports.in.ReportesUseCase;
import com.rentaherramientas.application.ports.out.ConsultaReportesPort;
import com.rentaherramientas.application.usecases.ReportesService;
import com.rentaherramientas.application.ports.out.AlmacenamientoArchivosPort;
import com.rentaherramientas.application.ports.out.AutenticadorPort;
import com.rentaherramientas.application.ports.out.ConsultaDevolucionesPort;
import com.rentaherramientas.application.ports.out.ConsultaFacturasPort;
import com.rentaherramientas.application.ports.out.ConsultaHerramientasPort;
import com.rentaherramientas.application.ports.out.ConsultaPagosPort;
import com.rentaherramientas.application.ports.out.GeneradorFacturaPdfPort;
import com.rentaherramientas.application.ports.out.PasarelaPagoPort;
import com.rentaherramientas.application.ports.out.ConsultaReservasPort;
import com.rentaherramientas.application.ports.out.PasswordEncoderPort;
import com.rentaherramientas.application.ports.out.TokenProviderPort;
import com.rentaherramientas.application.usecases.AutenticacionService;
import com.rentaherramientas.application.usecases.GestionCategoriasService;
import com.rentaherramientas.application.usecases.GestionClientesService;
import com.rentaherramientas.application.usecases.GestionDevolucionesService;
import com.rentaherramientas.application.usecases.GestionHerramientasService;
import com.rentaherramientas.application.usecases.GestionPagosService;
import com.rentaherramientas.application.usecases.GestionProveedoresService;
import com.rentaherramientas.application.usecases.GestionReservasService;
import com.rentaherramientas.domain.repositories.CategoriaRepository;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.DevolucionRepository;
import com.rentaherramientas.domain.repositories.EntregaRepository;
import com.rentaherramientas.domain.repositories.ReporteDanoRepository;
import com.rentaherramientas.domain.repositories.FacturaRepository;
import com.rentaherramientas.domain.repositories.PagoRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import com.rentaherramientas.domain.repositories.UsuarioRepository;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Clock;

/**
 * Registra los casos de uso como beans de Spring.
 *
 * <p>Las clases de la capa de aplicación son Java puro (sin @Service ni @Transactional),
 * así que aquí se crean con "new" y se envuelven en un <b>proxy transaccional</b>: cada
 * método del caso de uso se ejecuta dentro de una transacción. Si algo falla a mitad de
 * camino (ej. se guarda el usuario pero falla el cliente) se hace rollback de todo.</p>
 *
 * <p>Métodos que empiezan por obtener*, listar*, consultar*, cotizar* o descargar* usan transacciones de solo lectura;
 * el resto, transacciones de lectura y escritura.</p>
 */
@Configuration
@EnableConfigurationProperties({ReservasProperties.class, RecuperacionProperties.class})
public class UseCaseConfig {

    private final TransactionInterceptor transacciones;

    public UseCaseConfig(PlatformTransactionManager transactionManager) {
        DefaultTransactionAttribute escritura = new DefaultTransactionAttribute();
        DefaultTransactionAttribute lectura = new DefaultTransactionAttribute();
        lectura.setReadOnly(true);

        NameMatchTransactionAttributeSource reglas = new NameMatchTransactionAttributeSource();
        reglas.addTransactionalMethod("obtener*", lectura);
        reglas.addTransactionalMethod("listar*", lectura);
        reglas.addTransactionalMethod("consultar*", lectura);
        reglas.addTransactionalMethod("cotizar*", lectura);
        reglas.addTransactionalMethod("descargar*", lectura);
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

    @Bean
    public GestionReservasUseCase gestionReservasUseCase(ReservaRepository reservaRepository,
                                                         HerramientaRepository herramientaRepository,
                                                         ClienteRepository clienteRepository,
                                                         ProveedorRepository proveedorRepository,
                                                         ConsultaReservasPort consultaReservas,
                                                         ReservasProperties propiedades, Clock reloj) {
        PoliticaReservas politica = new PoliticaReservas(propiedades.plazoPago(), propiedades.diasMaximosAlquiler());
        return transaccional(new GestionReservasService(reservaRepository, herramientaRepository, clienteRepository,
                proveedorRepository, consultaReservas, politica, reloj), GestionReservasUseCase.class);
    }

    @Bean
    public GestionPagosUseCase gestionPagosUseCase(ReservaRepository reservaRepository, PagoRepository pagoRepository,
                                                   FacturaRepository facturaRepository,
                                                   ClienteRepository clienteRepository,
                                                   ProveedorRepository proveedorRepository, PasarelaPagoPort pasarela,
                                                   ConsultaPagosPort consultaPagos,
                                                   ConsultaFacturasPort consultaFacturas,
                                                   GeneradorFacturaPdfPort generadorPdf, Clock reloj) {
        return transaccional(new GestionPagosService(reservaRepository, pagoRepository, facturaRepository,
                clienteRepository, proveedorRepository, pasarela, consultaPagos, consultaFacturas, generadorPdf,
                reloj), GestionPagosUseCase.class);
    }

    @Bean
    public GestionDevolucionesUseCase gestionDevolucionesUseCase(ReservaRepository reservaRepository,
                                                                 EntregaRepository entregaRepository,
                                                                 DevolucionRepository devolucionRepository,
                                                                 ReporteDanoRepository reporteDanoRepository,
                                                                 HerramientaRepository herramientaRepository,
                                                                 ClienteRepository clienteRepository,
                                                                 ProveedorRepository proveedorRepository,
                                                                 ConsultaDevolucionesPort consulta, Clock reloj) {
        return transaccional(new GestionDevolucionesService(reservaRepository, entregaRepository, devolucionRepository,
                reporteDanoRepository, herramientaRepository, clienteRepository, proveedorRepository, consulta, reloj),
                GestionDevolucionesUseCase.class);
    }

    @Bean
    public ReportesUseCase reportesUseCase(ConsultaReportesPort consultaReportes,
                                           ProveedorRepository proveedorRepository, Clock reloj) {
        return transaccional(new ReportesService(consultaReportes, proveedorRepository, reloj), ReportesUseCase.class);
    }

    @Bean
    public RecuperacionPasswordUseCase recuperacionPasswordUseCase(UsuarioRepository usuarioRepository,
                                                                   TokenRecuperacionRepository tokenRepository,
                                                                   PasswordEncoderPort passwordEncoder,
                                                                   EnvioCorreoPort correo,
                                                                   RecuperacionProperties propiedades, Clock reloj) {
        PoliticaRecuperacion politica = new PoliticaRecuperacion(propiedades.vigencia(), propiedades.urlRestablecer());
        return transaccional(new RecuperacionPasswordService(usuarioRepository, tokenRepository, passwordEncoder,
                correo, politica, reloj), RecuperacionPasswordUseCase.class);
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
