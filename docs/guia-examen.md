# Guía de estudio para el examen — Renta de Herramientas

El 39.5 % de la nota es **"Dominio y conocimiento del código"**. Esta guía junta las preguntas de cada fase con una
respuesta modelo. Primero respóndelas con tus palabras y luego compara.

> Al cerrar cada fase nueva, agrega aquí sus 4 preguntas con su respuesta modelo.

---

## Fase 0 — Estructura y configuración

**1. ¿Por qué el pom padre tiene `packaging pom` y por qué la clase `main` está en `infrastructure`?**
El pom padre no genera código: agrupa los módulos y centraliza las versiones. El `main` va en `infrastructure`
porque usa `@SpringBootApplication` (Spring) y es el único módulo que puede conocer el framework; depende de
`application` y `domain`, así que puede ensamblar todo. El dominio no puede depender de Spring.

**2. ¿Qué hace `ddl-auto: validate` y por qué no `update` si hay Flyway?**
Al arrancar compara las entidades JPA contra las tablas reales (columnas y tipos) y, si no coinciden, la app no
arranca; nunca modifica nada. Con `update`, Hibernate y Flyway serían dos dueños del esquema y chocarían; además
`update` no borra columnas, no deja historial y no se reproduce en otra máquina. Flyway versiona cada cambio.

**3. ¿Por qué `prod` no tiene valor por defecto para `DB_URL` y `dev` sí?**
Principio *fail-fast*: en producción es mejor que la app **no arranque** a que arranque conectada a la base
equivocada. Sin valor por defecto, si falta la variable falla de inmediato y el error es evidente. En `dev`
`localhost` es una comodidad segura.

**4. Si cambian MySQL por PostgreSQL, ¿qué módulos tocas?**
Solo `infrastructure`: driver y `flyway-database-postgresql` en el pom, URL en los `.yml`/`.env`, migraciones SQL con
sintaxis propia de MySQL y el script de creación de BD. **`domain` y `application` no cambian**: hablan con interfaces
(puertos), no con MySQL. Ese es el beneficio de hexagonal.

---

## Fase 1 — Modelo de datos

**1. `reserva_detalles` guarda `precio_por_dia`, que ya está en `herramientas`. ¿Es redundancia mala?**
No: es una **foto histórica**. `herramientas.precio_por_dia` es el precio actual; el de `reserva_detalles` es el
pactado el día de la reserva. Si el proveedor sube el precio, las reservas y facturas viejas no deben cambiar.

**2. ¿Por qué los datos de prueba están en `db/seed` y no en `db/migration`?**
Flyway carga carpetas distintas según el perfil. En `prod` nunca llegan los usuarios de prueba, que tienen una clave
conocida por todos (`Renta2026*`): sería un hueco de seguridad.

**3. ¿Qué acelera `idx_reservas_estado_fechas` y por qué esas tres columnas?**
La consulta de disponibilidad: `WHERE estado IN (...) AND fecha_inicio <= :fin AND fecha_fin >= :inicio`. `estado` va
primero porque se compara por igualdad y las fechas después porque son rangos; MySQL usa el índice de izquierda a derecha.

**4. Ya aplicaste V3 y falta una columna en `reservas`. ¿Modificas V3?**
Nunca. Flyway guardó el checksum de V3 y `validate-on-migrate` detendría la app. Se crea una migración nueva
(`V10__agregar_columna_x.sql`) con un `ALTER TABLE`.

---

## Fase 2 — Seguridad con JWT

**1. ¿Por qué se deshabilita CSRF y por qué no es un riesgo?**
CSRF funciona porque el navegador envía las cookies **automáticamente**. El header `Authorization: Bearer` el navegador
nunca lo envía solo; lo pone nuestro código, así que una página maliciosa no puede falsificarlo. La API es stateless
(sin cookies de sesión).

**2. Alguien cambia `"roles":["CLIENTE"]` por `["ADMIN"]` en su token. ¿Qué pasa?**
**No tiene acceso: 401.** El payload solo está codificado en Base64 (se puede *leer*, no es secreto), pero la firma
HMAC se calcula con header + payload + `JWT_SECRET`. Si se altera el payload, la firma deja de coincidir y
`JwtTokenProvider.validar()` rechaza el token. Sin la clave secreta no se puede generar una firma válida. Por eso no se
guardan datos sensibles en el payload.

**3. ¿Diferencia entre 401 y 403?**
401 = "no sé quién eres" (sin token, token vencido, clave incorrecta). 403 = "sé quién eres, pero no puedes" (un CLIENTE
pide `GET /clientes`; pide el perfil de otro cliente; usuario inactivo).

**4. Los casos de uso no tienen `@Transactional`. ¿Cómo corren en una transacción?**
`UseCaseConfig` crea el caso de uso con `new` y lo envuelve en un **proxy** (`ProxyFactory` + `TransactionInterceptor`).
El controlador llama al proxy: abre la transacción, llama al método real y hace commit o rollback si hay excepción. Es el
mismo mecanismo de `@Transactional` (un proxy AOP) pero configurado desde infraestructura para que `application` siga
sin depender de Spring. Los métodos `obtener*`/`listar*` usan solo lectura.

---

## Fase 3 — Catálogo e imágenes

**1. ¿Por qué la regla de `/herramientas/inventario` va antes que la pública `/herramientas/**`?**
Spring Security evalúa los `requestMatchers` **en orden y gana el primero que coincide**. `/inventario` también calza con
`/herramientas/**`. Si se invierten, el inventario quedaría público: cualquiera sin token entraría al controlador, que
espera un usuario autenticado, y fallaría (o expondría datos).

**2. ¿Por qué la extensión sale del `contentType` y no del nombre del archivo? ¿Qué es *path traversal*?**
El nombre lo controla el usuario: podría mandar `../../x.jsp` o `virus.exe`. *Path traversal* es usar `../` en un nombre
para escapar de la carpeta permitida y escribir en otro lugar del disco. Se evita generando el nombre con **UUID** y
verificando con `normalize()` + `startsWith(raíz)` que la ruta final quede dentro de la carpeta base. *Matiz honesto:*
el `contentType` también lo declara el cliente; una validación más fuerte revisa los "magic bytes" del archivo.

**3. ¿Por qué un `ConsultaHerramientasPort` aparte del `HerramientaRepository`?**
CQRS ligero. La **escritura** pasa por el agregado, que protege los invariantes. La **lectura** del catálogo necesita
datos de varias tablas (categoría, proveedor, imágenes) en una sola consulta optimizada, devuelta directo como DTO, sin
construir agregados completos.

**4. ¿Qué hace `orphanRemoval = true` en la lista de imágenes?**
Si una imagen sale de la lista de la entidad padre, Hibernate la elimina de la tabla (`DELETE`) automáticamente. Sin
eso quedarían filas huérfanas.

---

## Fase 4 — Reservas y disponibilidad

**1. ¿Por qué una reserva `PENDIENTE` no aparta unidades y una `APROBADA` sí?**
Porque en `PENDIENTE` el proveedor todavía no se comprometió: si apartara, un cliente podría bloquear el inventario sin
compromiso (y se bloquearía con solicitudes falsas). En `APROBADA` hay compromiso del proveedor y un plazo para pagar;
si no se paga, vence y libera las unidades.

**2. ¿Qué evita `SELECT ... FOR UPDATE` al aprobar? ¿Por qué se bloquea ordenado por id?**
Una **condición de carrera**: dos aprobaciones leen "queda 1 unidad" al mismo tiempo y ambas aprueban → sobreventa.
Con el bloqueo, la segunda espera, vuelve a verificar y ve 0. Se ordena por id para evitar **deadlocks**: si una
transacción bloquea A y luego B y otra bloquea B y luego A, se quedan esperándose; con el mismo orden siempre, no.

**3. ¿Por qué el caso de uso recibe un `Clock`?**
Para poder **fijar "hoy"** en las pruebas (`Clock.fixed`) y probar vencimientos y validaciones de fechas sin depender del
reloj real; además centraliza la zona horaria (`America/Bogota`).

**4. Explica `fechaInicio <= finPedido AND fechaFin >= inicioPedido`.**
Dos rangos se cruzan si cada uno empieza antes de que termine el otro. *Se cruzan:* A = 20–22 y B = 22–25 (comparten el
22): `20 <= 25` y `22 >= 22`. *No se cruzan:* A = 20–22 y B = 23–25: `22 >= 23` es falso.

---

## Fase 5 — Pagos y facturación

**1. ¿Por qué `verificarQueSePuedePagar()` va **antes** de cobrar si `confirmarPago()` ya valida lo mismo?**
Porque `confirmarPago()` se ejecuta **después** del cobro. Si solo validáramos ahí, ya le habríamos cobrado dinero real a
una reserva vencida o ya pagada y habría que reembolsar. Validar antes evita cobrar algo que no se puede confirmar. La
segunda validación queda como defensa del dominio: el invariante siempre está protegido.

**2. ¿Por qué se guardan también los pagos rechazados?**
Auditoría y soporte (qué pasó con el intento del cliente), detección de fraude (muchos rechazos seguidos) y trazabilidad
completa del historial. El cliente puede reintentar y todos los intentos quedan registrados.

**3. ¿Por qué no se guarda el número de tarjeta? Si usan Wompi, ¿qué cambia?**
Guardar datos de tarjeta exige cumplir **PCI DSS** y, si la BD se filtra, el daño es enorme. Solo se guarda la
**referencia de la transacción**. Con Wompi se crea un nuevo adaptador (`WompiPasarelaAdapter`) que implemente
`PasarelaPagoPort` en `infrastructure/adapters/outbound/pagos`, con sus claves en variables de entorno, y se desactiva el
simulado (por ejemplo con `@Profile`). `domain` y `application` **no cambian**.

**4. ¿Qué pasaría sin el bloqueo de la reserva si el cliente hace doble clic en "Pagar"?**
Dos peticiones simultáneas leerían la reserva como `APROBADA`, ambas pasarían la validación y ambas cobrarían: **doble
cobro** y dos facturas. Con el bloqueo, la segunda espera, entra cuando la reserva ya es `PAGADA` y recibe un 422.

---

## Fase 6 — Entregas, devoluciones y daños

**1. ¿Por qué `Entrega`, `Devolucion` y `ReporteDano` son agregados distintos y no listas dentro de `Reserva`?**
Porque tienen su propio ciclo de vida y sus propios responsables: la devolución la confirma el proveedor y la valida
el ADMIN; el daño pasa por `ABIERTO → EN_REVISION → RESUELTO` mucho después de cerrada la reserva. Si fueran parte de
`Reserva`, cada cambio de una incidencia obligaría a cargar y guardar toda la reserva. La `Reserva` solo controla su
estado (`PAGADA → ENTREGADA → DEVUELTA`) con `registrarEntrega()` y `registrarDevolucion()`.

**2. ¿Qué pasa en el sistema cuando el proveedor reporta un daño GRAVE? Recorre las capas.**
`DevolucionController` recibe la petición → `GestionDevolucionesService.registrarDano()` crea el reporte con
`ReporteDano.reportar()` (el dominio valida que la herramienta estaba en la reserva y que el equipo no se devolvió
`BUENO`) → como `SeveridadDano.GRAVE.sacaDeServicio()` es `true`, bloquea la fila de la herramienta con
`SELECT ... FOR UPDATE` y llama `Herramienta.sacarDeServicio(1)` → `unidades_fuera_servicio` sube en 1 → la
disponibilidad de esa herramienta baja, porque se calcula con `stock − fuera de servicio − comprometidas`.
Al **resolver** la incidencia, `reintegrarAServicio(1)` devuelve la unidad reparada.

**3. ¿Por qué `validarDevolucion` revisa `esAdmin()` en el caso de uso si el controlador ya tiene
`@PreAuthorize("hasRole('ADMIN')")`?**
Defensa en profundidad. `@PreAuthorize` es infraestructura (Spring): si mañana el caso de uso se llama desde otro
adaptador (un job, un mensaje, una prueba) esa anotación no existe. La regla "solo el ADMIN valida" es del negocio, así
que también vive en la capa de aplicación, que no depende de Spring.

**4. En las pruebas, un token con una "x" pegada al final seguía funcionando. ¿Es una vulnerabilidad?**
No. La firma HS384 son 48 bytes = 64 caracteres Base64 exactos. Un carácter más aporta solo 6 bits, que no completan
un byte, y el decodificador los descarta: la firma decodificada es **idéntica**. Lo que importa es que no se puede
cambiar el **contenido**: si se altera cualquier carácter real de la firma, o el payload (por ejemplo `"roles":["ADMIN"]`),
la firma HMAC calculada con `JWT_SECRET` ya no coincide y la respuesta es 401. El script de pruebas hace justo ese
ataque (cambia el rol a ADMIN) y verifica el 401.

---

## Fase 7 — Reportes y estadísticas

**1. ¿Por qué los reportes usan SQL nativo con `NamedParameterJdbcTemplate` y no repositorios JPA?**
Los reportes son **agregaciones** (`SUM`, `COUNT`, `GROUP BY`) que no corresponden a ninguna entidad. Con SQL la base de
datos hace el cálculo y solo viajan los totales; con JPA habría que cargar cientos de entidades a memoria para sumarlas
en Java. Es el lado de **lectura** del CQRS ligero: no pasa por los agregados porque no modifica nada. Y como es un
adaptador detrás de `ConsultaReportesPort`, el caso de uso no sabe ni le importa que sea SQL.

**2. Un PROVEEDOR llama `GET /reportes/resumen?proveedorId=5`. ¿Qué datos recibe y dónde se decide?**
Recibe **solo los suyos**. `ReportesService.alcance()` ignora el `proveedorId` que manda un proveedor y usa el de su
propio perfil (lo busca con el `usuarioId` del token). Solo el ADMIN puede elegir proveedor. Si fuera un CLIENTE,
`@PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")` responde 403 antes de llegar al caso de uso (y el caso de uso
también lo rechaza: defensa en profundidad).

**3. ¿Qué diferencia hay entre ingresos brutos y netos, y por qué se reportan por fecha de pago?**
*Brutos* = lo que pagó el cliente (incluye IVA del 19 %). *Netos* = subtotal sin IVA. El IVA se recauda para la DIAN,
no es ingreso del negocio, así que la rentabilidad se mide con el neto. Se usa la fecha del pago aprobado (criterio de
**caja**): el dinero entra cuando se paga, no cuando se crea la reserva.

**4. ¿Dónde se calcula la ocupación de una herramienta y por qué ahí y no en el SQL?**
En `ReportesService.consultarRentabilidad()`: `unidades-día alquiladas / (stock × días del periodo) × 100`. El SQL
entrega cifras crudas (unidades-día, stock) y la regla de negocio (cómo se define "ocupación", el redondeo, el orden por
utilidad) queda en la capa de aplicación, donde se puede probar con Mockito sin base de datos.

**Trampa que apareció:** los *text blocks* de Java (`"""`) eliminan los espacios al final de cada línea. Por eso
`"WHERE """ + condicion` producía `WHEREr.estado` y MySQL respondía error de sintaxis. Se resolvió poniendo los
espacios dentro de las constantes que se concatenan.

---

## Fase 8 — Pruebas y documentación

**1. ¿Qué diferencia hay entre las pruebas de dominio, las de casos de uso y las de integración del proyecto?**
*Dominio* (`ReservaTest`, `DineroTest`…): Java puro, prueban reglas sin Spring ni base de datos; corren en milisegundos.
*Casos de uso* (`GestionPagosServiceTest`…): usan **Mockito** para simular los puertos (repositorios, pasarela) y
verifican la orquestación, por ejemplo que **nunca se llame a la pasarela** si la reserva no está APROBADA
(`verifyNoInteractions(pasarela)`). *Integración* (`FlujoAlquilerIntegracionTest`): levantan la app completa (Spring
Security, JPA, MySQL) y hacen peticiones HTTP con MockMvc; prueban que todas las piezas encajan.

**2. ¿Por qué las pruebas de integración no ensucian la base de datos y por qué no corren con `mvn test`?**
Usan una base aparte (`renta_herramientas_test`) y cada prueba es `@Transactional`: al terminar se hace **rollback**.
No corren por defecto porque necesitan MySQL con esa base; se excluyen con `@Tag("integracion")` en el `pom` y se
activan con `mvn verify -Pintegracion`. Así `mvn clean install` funciona en cualquier máquina.

**3. ¿Cómo se arma MockMvc sin la dependencia `spring-security-test`, y por qué importa?**
`MockMvcBuilders.webAppContextSetup(contexto).addFilters(springSecurityFilterChain)`. Se le agrega la cadena de
filtros de Spring Security para que cada petición pase por el `JwtValidationFilter` igual que en producción; sin eso
la prueba no comprobaría la seguridad.

**4. ¿Qué arregló `OpenApiErroresConfig` en Swagger?**
springdoc mostraba en los 401/403/404/422 el esquema del 200, que es falso. Un `OpenApiCustomizer` recorre todas las
operaciones y pone en cada 4xx/5xx el esquema `ProblemDetail` con un ejemplo realista, agrega 401/403 a las rutas
protegidas y quita el candado a las públicas. Se hace en un solo lugar en vez de repetir anotaciones en 60 endpoints.

---

## Fase 9 — Frontend

**1. Si un usuario cambia en `localStorage` su rol de CLIENTE a ADMIN, ¿qué consigue?**
Solo ver el menú del panel de ADMIN. Cada petición lleva el **JWT original**, cuyo rol está firmado por el servidor:
`@PreAuthorize("hasRole('ADMIN')")` responde 403 y el panel muestra el error. El rol en el navegador solo decide qué
pantalla dibujar; quien autoriza es siempre el backend.

**2. ¿Qué es `esc()` y por qué todo dato de la API pasa por ahí?**
Escapa `< > & " '` antes de insertar texto con `innerHTML`. Si un proveedor registra una herramienta llamada
`<img src=x onerror=alert(1)>`, sin `esc()` ese código se ejecutaría en el navegador de cada cliente (XSS) y podría
robar su token. Con `esc()` se muestra como texto.

**3. ¿Por qué la factura se descarga con `fetch` + `Blob` y no con un enlace `<a href>`?**
Porque el endpoint del PDF está protegido y un enlace normal no envía el header `Authorization: Bearer`. `descargar()`
hace la petición con el token, recibe los bytes, crea una URL temporal con `URL.createObjectURL` y simula el clic.

**4. ¿Qué pasa en el frontend cuando el token vence?**
La API responde 401. `api.js` lo detecta, cierra la sesión (`sesion.cerrar()`) y lleva a `ingresar.html?expirada=1`
recordando la página en `volver`; al iniciar sesión de nuevo regresa a donde estaba.

---

## Preguntas por archivo ("¿qué hace y por qué está ahí?")

| Archivo | Respuesta modelo corta |
|---|---|
| `UseCaseConfig` | Crea los casos de uso con `new` (porque `application` no tiene Spring) y los envuelve en un proxy con `TransactionInterceptor`: los métodos `obtener*/listar*/consultar*/cotizar*/descargar*` son de solo lectura; el resto, lectura y escritura. Está en `infrastructure/config` porque es "cableado" de Spring. |
| `Reserva` | Agregado raíz del alquiler: calcula totales y controla las transiciones de estado con métodos de negocio (`aprobar`, `confirmarPago`…), que validan el estado actual. Está en `domain` porque son reglas puras del negocio. |
| `ReservaRepository` vs. `ReservaJpaRepository` | El primero es el **puerto** (interfaz en el dominio: qué se necesita). El segundo es Spring Data (cómo se consulta). `ReservaRepositoryAdapter` une ambos y traduce entidad ↔ dominio. |
| `ConsultaReservasPort` | Puerto de **lectura** (CQRS ligero): devuelve `ReservaResult` con cliente, proveedor y detalles en una consulta optimizada (`@EntityGraph`), sin pasar por el agregado. |
| `JwtValidationFilter` | Filtro que corre una vez por petición: lee el Bearer, valida firma y expiración y pone el usuario en el `SecurityContext`. Está en `security` porque es infraestructura de Spring Security. |
| `GlobalExceptionHandler` | Traduce excepciones del dominio y de la aplicación a `ProblemDetail` con el código HTTP correcto. Está en `adapters/inbound` porque es parte de la entrada HTTP. |
| `PasarelaPagoSimuladaAdapter` | Implementa `PasarelaPagoPort`. Es un adaptador de salida: para usar Wompi se escribe otro adaptador del mismo puerto y el caso de uso no cambia. |
| `VencimientoReservasJob` | Adaptador de **entrada** (no HTTP): cada 5 minutos llama al caso de uso que vence las reservas APROBADAS sin pago. |
| `Dinero` | Value object: monto en `BigDecimal` con 2 decimales, inmutable. Evita los errores de `double` con dinero y centraliza el cálculo del IVA. |
| `TiempoConfig` | Publica un `Clock` de Bogotá. Los casos de uso piden la hora al `Clock` y no a `LocalDate.now()`, así en las pruebas se fija "hoy" en una fecha conocida. |
| `ConsultaReportesAdapter` | SQL nativo con `NamedParameterJdbcTemplate` para los reportes: agregaciones que no corresponden a ninguna entidad. |
| `OpenApiErroresConfig` | Personaliza Swagger para que todos los errores muestren `ProblemDetail`. |
| `V3__create_rental_tables.sql` | Migración Flyway que crea reservas, detalles, entregas, devoluciones y daños con sus `CHECK` y llaves foráneas. Nunca se edita: un cambio va en una V10 nueva. |
| `frontend/assets/js/api.js` | Cliente HTTP del frontend: agrega el JWT, convierte errores ProblemDetail y maneja el 401. |
