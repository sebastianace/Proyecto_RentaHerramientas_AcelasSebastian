# Mapa del código

Guía para encontrar y explicar cualquier archivo del proyecto. Rutas relativas a
`src/main/java/com/rentaherramientas/` de cada módulo.

**Rúbrica:** `ARQ` diseño y arquitectura · `CRUD` CRUDs y lógica · `SEG` seguridad · `JWT` implementación JWT ·
`PAG` pagos y facturación · `RES` reservas y disponibilidad · `REP` reportes · `BD` base de datos · `DOC` Swagger ·
`FRONT` frontend.

---

## 1. ¿Dónde está…? (índice rápido)

| Pregunta | Archivo · método |
|---|---|
| ¿Dónde se **firma** el JWT? | `infrastructure/security/JwtTokenProvider` · `generar()` (HMAC con `JWT_SECRET`) |
| ¿Dónde se **valida** el JWT en cada petición? | `infrastructure/security/JwtValidationFilter` · `doFilterInternal()` → `JwtTokenProvider.validar()` |
| ¿Dónde se decide qué rutas son públicas? | `infrastructure/security/SecurityConfig` · `requestMatchers(...)` (el orden importa) |
| ¿Dónde se responde 401 / 403 en formato ProblemDetail? | `security/RestAuthenticationEntryPoint` (401) y `security/RestAccessDeniedHandler` (403) |
| ¿Dónde se valida la contraseña al iniciar sesión? | `application/usecases/AutenticacionService` · `iniciarSesion()` → `security/SpringSecurityAutenticadorAdapter` · `autenticar()` (BCrypt) |
| ¿Dónde se calcula la **disponibilidad**? | `application/usecases/GestionReservasService` · `unidadesDisponibles()` = operativas − `ReservaRepository.unidadesComprometidas()` |
| ¿Dónde está la consulta SQL de unidades comprometidas? | `infrastructure/.../ReservaJpaRepository` · `sumarUnidadesComprometidas()` (APROBADA, PAGADA, ENTREGADA que se cruzan) |
| ¿Dónde se **bloquean las herramientas** al aprobar? | `GestionReservasService.aprobar()` → `HerramientaRepository.bloquearParaReserva()` → `HerramientaJpaRepository.bloquearPorIds()` (`@Lock(PESSIMISTIC_WRITE)`, ordenadas por id) |
| ¿Dónde se **bloquea la reserva** al pagar? | `GestionPagosService.pagar()` → `ReservaRepository.bloquearPorId()` → `ReservaJpaRepository.bloquearPorId()` |
| ¿Dónde se calculan subtotal, IVA y total? | `domain/model/aggregates/Reserva` (constructor) y `DetalleReserva` (precio × cantidad × días); IVA con `Dinero.porcentaje(19)` |
| ¿Dónde se cuentan los días (inclusivos)? | `domain/model/valueobject/RangoFechas` · `dias()` |
| ¿Dónde están las **transiciones de estado** de una reserva? | `domain/model/aggregates/Reserva` · `aprobar()`, `rechazar()`, `cancelar()`, `confirmarPago()`, `registrarEntrega()`, `registrarDevolucion()`, `vencerSiCorresponde()` |
| ¿Dónde se **vencen** las reservas sin pago? | `infrastructure/adapters/inbound/scheduler/VencimientoReservasJob` (`@Scheduled` cada 5 min) → `GestionReservasService.vencerReservasSinPago()` |
| ¿Dónde se **cobra**? | `application/usecases/GestionPagosService` · `pagar()` → `PasarelaPagoPort.cobrar()` → `adapters/outbound/pagos/PasarelaPagoSimuladaAdapter` · `cobrar()` |
| ¿Dónde se decide si la tarjeta se aprueba? | `PasarelaPagoSimuladaAdapter.cobrar()` (termina en 0000 → fondos, 1111 → banco) |
| ¿Dónde se **genera la factura**? | `GestionPagosService.pagar()` → `Factura.emitir()` + `FacturaRepository.generarNumero()` |
| ¿Dónde se arma el **PDF**? | `adapters/outbound/pdf/FacturaPdfAdapter` (OpenPDF), llamado por `GestionPagosService.descargarFacturaPdf()` |
| ¿Dónde un daño GRAVE saca unidades de servicio? | `GestionDevolucionesService` · `registrarDano()` → `Herramienta.sacarDeServicio(1)` |
| ¿Dónde se valida que un cliente solo vea lo suyo? | En cada caso de uso: `validarPuedeVer()`, `validarEsProveedorDe()`, `clienteIdDe()`, `alcance()` (reportes) |
| ¿Dónde se abren y cierran las **transacciones**? | `infrastructure/config/UseCaseConfig` · `transaccional()` (proxy con `TransactionInterceptor`) |
| ¿Dónde se traducen excepciones a códigos HTTP? | `infrastructure/adapters/inbound/exceptionhandler/GlobalExceptionHandler` |
| ¿Dónde se calculan los reportes? | SQL en `ConsultaReportesAdapter`; margen, ocupación y alertas en `ReportesService` |
| ¿Dónde se documentan los errores en Swagger? | `infrastructure/config/OpenApiErroresConfig` |
| ¿Dónde se configura CORS? | `security/SecurityConfig` + `security/CorsProperties` (`CORS_ALLOWED_ORIGINS`) |
| ¿Dónde se guarda el "hoy" de Bogotá? | `infrastructure/config/TiempoConfig` · `reloj()` (bean `Clock`) |
| ¿Dónde se crean las tablas? | `infrastructure/src/main/resources/db/migration/V1–V5` (Flyway); datos de prueba en `db/seed/V6–V9` |
| ¿Dónde guarda el frontend el JWT? | `frontend/assets/js/sesion.js` (localStorage); se envía en `api.js` · `api()` |

---

## 2. Módulo `domain` (Java puro: reglas del negocio) — `ARQ`

No depende de Spring ni de JPA. Si mañana se cambia MySQL o el framework, este módulo no se toca.

### `domain/model/aggregates` — agregados (objetos con identidad y reglas)

| Clase | Qué hace | Por qué aquí | Se relaciona con | Rúbrica |
|---|---|---|---|---|
| `Usuario` | Cuenta de acceso: email, hash de la clave, roles, activo | Regla: un usuario inactivo no entra | `Cliente`, `Proveedor`, `RolNombre`, `Email` | SEG |
| `Cliente` | Perfil de cliente (documento, dirección, ciudad) | Datos del negocio, no de la tabla | `Usuario`, `TipoDocumento` | CRUD |
| `Proveedor` | Perfil de proveedor (razón social, NIT) | Ídem | `Usuario`, `Herramienta` | CRUD |
| `Categoria` | Categoría del catálogo, activable | Regla de activo/inactivo | `Herramienta` | CRUD |
| `Herramienta` | Inventario: precio, stock, unidades fuera de servicio, estado, imágenes. `unidadesOperativas()`, `admiteReservas()`, `sacarDeServicio()`, `reintegrarAServicio()` | Regla: fuera de servicio ≤ stock; máx. 8 imágenes | `ImagenHerramienta`, `Reserva`, `ReporteDano` | CRUD, RES |
| `ImagenHerramienta` | Foto de una herramienta (URL, principal, orden) | Parte del agregado `Herramienta` | `Herramienta` | CRUD |
| `Reserva` | **Raíz del alquiler**: rango de fechas, detalles, totales y ciclo de estados | Todas las transiciones validan el estado actual | `DetalleReserva`, `RangoFechas`, `Dinero`, `Pago` | RES |
| `DetalleReserva` | Línea: herramienta, cantidad, precio **congelado**, días, subtotal | El precio se copia al reservar | `Reserva` | RES |
| `Pago` | Intento de pago (aprobado o rechazado), sin datos de tarjeta | Regla: monto = total de la reserva | `Reserva`, `MetodoPago` | PAG |
| `Factura` | Factura emitida por un pago aprobado. `emitir()` | Regla: solo se factura lo pagado | `Pago`, `Reserva` | PAG |
| `Entrega` | Entrega del equipo; a domicilio exige dirección | Regla de negocio de la dirección | `Reserva`, `TipoEntrega` | RES |
| `Devolucion` | Devolución confirmada por el proveedor y validada por el ADMIN (`validar()` una sola vez) | Dos responsables, regla de validar una vez | `Reserva`, `ReporteDano` | RES |
| `ReporteDano` | Daño encontrado. `reportar()` exige equipo no BUENO y herramienta de la reserva; `avanzarA()` | Reglas del reporte y de la incidencia | `Devolucion`, `Herramienta` | RES |

### `domain/model/valueobject` — objetos de valor (sin identidad, inmutables)

| Clase | Qué hace | Rúbrica |
|---|---|---|
| `Dinero` | Monto en `BigDecimal` con 2 decimales; `sumar`, `multiplicar`, `porcentaje` (IVA) | RES, PAG |
| `RangoFechas` | Inicio–fin; `dias()` inclusivo y `seCruzaCon()` | RES |
| `Email` | Valida y normaliza el correo | CRUD |
| `EstadoReserva` | PENDIENTE, APROBADA, PAGADA, ENTREGADA, DEVUELTA, RECHAZADA, CANCELADA, VENCIDA | RES |
| `EstadoPago`, `MetodoPago`, `EstadoFactura` | Estados y medios de pago (TARJETA_CREDITO, TARJETA_DEBITO, PSE, NEQUI) | PAG |
| `EstadoHerramienta` | ACTIVA, EN_MANTENIMIENTO, INACTIVA (borrado lógico) | CRUD |
| `TipoEntrega` | DOMICILIO / RECOGIDA_EN_SEDE; `requiereDireccion()` | RES |
| `EstadoEquipo` | BUENO, DESGASTE_NORMAL, CON_DANOS; `admiteReporteDeDano()` | RES |
| `SeveridadDano` | LEVE, MODERADO, GRAVE; `sacaDeServicio()` | RES |
| `EstadoIncidencia` | ABIERTO → EN_REVISION → RESUELTO; `puedePasarA()` | RES |
| `RolNombre` | ADMIN, PROVEEDOR, CLIENTE | SEG |
| `TipoDocumento` | CC, CE, NIT, PASAPORTE | CRUD |

### `domain/repositories` — puertos de persistencia (interfaces)

`UsuarioRepository`, `ClienteRepository`, `ProveedorRepository`, `CategoriaRepository`, `HerramientaRepository`
(incluye `bloquearParaReserva`), `ReservaRepository` (incluye `bloquearPorId`, `unidadesComprometidas`,
`buscarAprobadasConPagoVencido`, `generarCodigo`), `PagoRepository`, `FacturaRepository`, `EntregaRepository`,
`DevolucionRepository`, `ReporteDanoRepository`. **Por qué aquí:** el dominio dice QUÉ necesita guardar; la
infraestructura decide CÓMO (JPA). Paginación propia: `SolicitudPagina` y `Pagina<T>` (para no depender de Spring Data).

### `domain/exceptions`

`DominioException` (base) · `RecursoNoEncontradoException` → 404 · `ConflictoException` → 409 ·
`ReglaDeNegocioException` → 422 (incluye `requerido()` y `opcional()` para validar textos).

---

## 3. Módulo `application` (Java puro: casos de uso) — `ARQ`

Orquesta el dominio y los puertos. No conoce Spring: las transacciones se las pone `UseCaseConfig` desde afuera.

### `ports/in` (lo que la aplicación ofrece) y `usecases` (su implementación)

| Puerto de entrada | Implementación | Qué hace | Rúbrica |
|---|---|---|---|
| `AutenticacionUseCase` | `AutenticacionService` | Login, registro de cliente, perfil (`/me`) | SEG, JWT |
| `GestionClientesUseCase` | `GestionClientesService` | CRUD de clientes (el cliente solo edita su perfil) | CRUD |
| `GestionProveedoresUseCase` | `GestionProveedoresService` | CRUD de proveedores | CRUD |
| `GestionCategoriasUseCase` | `GestionCategoriasService` | CRUD de categorías | CRUD |
| `GestionHerramientasUseCase` | `GestionHerramientasService` | Catálogo, inventario, CRUD, estado e imágenes | CRUD |
| `GestionReservasUseCase` | `GestionReservasService` | Disponibilidad, cotización, crear, aprobar, rechazar, cancelar, vencer | RES |
| `GestionPagosUseCase` | `GestionPagosService` | Pagar, historial de pagos, facturas y PDF | PAG |
| `GestionDevolucionesUseCase` | `GestionDevolucionesService` | Entrega, devolución, daños, validación e incidencias | RES |
| `ReportesUseCase` | `ReportesService` | Panel, resumen, ingresos, rankings, rentabilidad, inventario | REP |

### `ports/out` (lo que la aplicación necesita del exterior)

| Puerto | Lo implementa (infrastructure) | Para qué |
|---|---|---|
| `AutenticadorPort` | `SpringSecurityAutenticadorAdapter` | Verificar email + clave |
| `TokenProviderPort` | `JwtTokenProvider` | Generar y validar JWT |
| `PasswordEncoderPort` | `BCryptPasswordEncoderAdapter` | Hashear claves |
| `PasarelaPagoPort` | `PasarelaPagoSimuladaAdapter` | Cobrar (cambiable por Wompi/PayU) |
| `GeneradorFacturaPdfPort` | `FacturaPdfAdapter` | PDF de la factura |
| `AlmacenamientoArchivosPort` | `AlmacenamientoLocalAdapter` | Guardar imágenes |
| `ConsultaHerramientasPort`, `ConsultaReservasPort`, `ConsultaPagosPort`, `ConsultaFacturasPort`, `ConsultaDevolucionesPort`, `ConsultaReportesPort` | `Consulta*Adapter` | **Lado de lectura (CQRS ligero):** datos de varias tablas en una consulta |

### Otros paquetes

- `dtos`: `*Command` (lo que entra), `*Result` (lo que sale), filtros (`Filtro*`), `Solicitante` (quién pide, sale del
  token), `SolicitudCobro`/`ResultadoCobro` (contrato con la pasarela), `ReportesResult` (todas las formas de los reportes).
- `config/PoliticaReservas`: plazo de pago (24 h) y días máximos (90), traídos de `application.yml`.
- `exceptions`: `AccesoDenegadoException` y `UsuarioInactivoException` → 403, `CredencialesInvalidasException` → 401.
- `mappers`: `IdentidadMapper`, `CatalogoMapper` (dominio → `*Result`, mapeo manual).

---

## 4. Módulo `infrastructure` (Spring Boot: adaptadores) — `ARQ`

### `adapters/inbound/controller` — controladores REST (sin lógica de negocio)

| Controlador | Rutas | Rúbrica |
|---|---|---|
| `AuthController` | `/auth/login`, `/auth/registro`, `/auth/me` | JWT |
| `ClienteController`, `ProveedorController` | `/clientes`, `/proveedores` (CRUD + activar) | CRUD |
| `CategoriaController` | `/categorias` | CRUD |
| `HerramientaController` | `/herramientas` (catálogo, inventario, CRUD, estado, imágenes) | CRUD |
| `DisponibilidadController` | `/herramientas/{id}/disponibilidad` | RES |
| `ReservaController` | `/reservas` (+ cotización, aprobar, rechazar, cancelar) | RES |
| `PagoController`, `FacturaController` | `/reservas/{id}/pagos`, `/pagos`, `/facturas` (+ `/pdf`) | PAG |
| `DevolucionController` | `/reservas/{id}/entrega`, `/reservas/{id}/devolucion`, `/devoluciones`, `/danos` | RES |
| `ReporteController` | `/reportes/*` | REP |

Cada uno: recibe, valida el formato (`@Valid`), protege con `@PreAuthorize`, llama al caso de uso y responde.
Documentados con `@Operation`, `@ApiResponse` y `@Tag` (DOC).

### Otros inbound

- `dtos/*Request`: cuerpo HTTP con Bean Validation (`@NotNull`, `@Email`, `@Pattern`…) y `@Schema(example = …)`.
  `aCommand()` los convierte a comandos de aplicación. `ValidacionesComunes` guarda las regex compartidas.
- `exceptionhandler/GlobalExceptionHandler`: excepción → `ProblemDetail` (404, 409, 422, 401, 403, 400 con `errores`).
- `scheduler/VencimientoReservasJob`: tarea `@Scheduled` que vence las reservas aprobadas sin pago (RES).

### `adapters/outbound/persistence` — JPA (BD)

- `entities/*Entity`: una por tabla. Enums con `@Enumerated(STRING)` + `@JdbcTypeCode(VARCHAR)`; fechas de MySQL con
  `insertable=false, updatable=false` (y `@Generated` cuando hay que leerlas tras el INSERT, ej. `ReporteDanoEntity`).
- `repositories/*JpaRepository`: Spring Data (consultas JPQL, `@EntityGraph`, `@Lock`).
- `repositories/*RepositoryAdapter`: implementan los puertos del dominio; cargan la entidad y le copian los cambios.
- `repositories/Consulta*Adapter`: lado de lectura. `ConsultaReportesAdapter` usa **SQL nativo** con
  `NamedParameterJdbcTemplate` (SUM/COUNT/GROUP BY).
- `mappers/*EntityMapper`: entidad ↔ dominio ↔ `*Result`. `PaginaMapper`: `Page` de Spring ↔ `Pagina` propia.
  `RolResolver`: obtiene la fila de rol por su nombre.

### Otros outbound

- `pagos/PasarelaPagoSimuladaAdapter` (PAG), `pdf/FacturaPdfAdapter` (PAG, marca `ObraYa`),
  `storage/AlmacenamientoLocalAdapter` + `MediaProperties` (imágenes en `MEDIA_BASE_PATH`, servidas en `/media`).

### `security` — `SEG`, `JWT`

`SecurityConfig` (stateless, CSRF off, rutas públicas, filtro JWT, `@EnableMethodSecurity`, CORS) ·
`JwtTokenProvider` (firma/valida con jjwt) · `JwtValidationFilter` (lee `Authorization: Bearer`, valida y pone el
usuario en el contexto) · `UsuarioAutenticado` (principal; `aSolicitante()`) · `UserDetailsServiceImpl` (carga el
usuario para el login) · `SpringSecurityAutenticadorAdapter` · `BCryptPasswordEncoderAdapter` ·
`RestAuthenticationEntryPoint` (401) · `RestAccessDeniedHandler` (403) · `JwtProperties`, `CorsProperties`.

### `config`

`UseCaseConfig` (crea los casos de uso con `new` y los envuelve en transacciones) · `TiempoConfig` (`Clock` de Bogotá) ·
`ReservasProperties` (`app.reservas.*`) · `MediaConfig` (sirve `/media/**`) · `OpenApiConfig` (Swagger + Authorize) ·
`OpenApiErroresConfig` (errores con ProblemDetail).

---

## 5. Base de datos (`infrastructure/src/main/resources/db`) — `BD`

- `migration/V1` identidad · `V2` catálogo · `V3` reservas, entregas, devoluciones y daños · `V4` pagos y facturas ·
  `V5` índices. **Nunca se editan** (Flyway valida su checksum).
- `seed/V6–V9`: datos de prueba (solo perfil `dev`).
- `database/00_create_database_and_user.sql`: bases `renta_herramientas` y `renta_herramientas_test` y el usuario.

---

## 6. Frontend (`frontend/`) — `FRONT`

| Archivo | Qué hace |
|---|---|
| `assets/js/api.js` | `fetch` con el JWT; errores ProblemDetail → `ApiError`; 401 → cierra sesión |
| `assets/js/sesion.js` | Token y usuario en localStorage; `rol()`, `exigir()` |
| `assets/js/carrito.js` | Carrito (un solo proveedor por reserva) |
| `assets/js/ui.js` | `esc()` (anti-XSS), modales, toasts, layout, carrito lateral con cotización en vivo |
| `assets/js/iconos.js` | Íconos e ilustraciones SVG por categoría |
| `assets/js/graficas.js` | Columnas y barras SVG con tooltip y vista de tabla |
| `assets/js/paginas/*.js` | Una por página: tienda, herramienta, reserva, ingresar, panel |
| `assets/js/secciones/*.js` | Secciones del panel: comunes, cliente, proveedor, admin, tablero, devoluciones |
| `assets/css/app.css` | Sistema de diseño con tokens y tema claro/oscuro |

## 7. Pruebas

`domain/src/test` (reglas puras) · `application/src/test` (casos de uso con Mockito) ·
`infrastructure/src/test` (`IntegracionBase`, `SeguridadIntegracionTest`, `FlujoAlquilerIntegracionTest`) ·
`docs/pruebas/flujo-completo.ps1` · `docs/postman/`.
