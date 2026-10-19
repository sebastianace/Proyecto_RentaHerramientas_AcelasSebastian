# Flujos principales: del clic a la tabla

Cada flujo recorre las capas en orden: **Frontend → Controlador → Caso de uso → Dominio → Adaptador → Tabla**.
Si en la sustentación te preguntan "¿qué pasa cuando…?", contesta con este recorrido.

---

## 1. Login con JWT

```
ingresar.js ──POST /api/v1/auth/login {email, password}──►
  JwtValidationFilter      (no hay token: deja pasar, la ruta es pública en SecurityConfig)
  AuthController.login()   (@Valid LoginRequest)
  AutenticacionService.iniciarSesion()
    ├─ AutenticadorPort.autenticar()  → SpringSecurityAutenticadorAdapter
    │     AuthenticationManager + UserDetailsServiceImpl + BCrypt   ── tablas: usuarios, usuario_roles, roles
    │     clave incorrecta → CredencialesInvalidasException → 401
    │     usuario inactivo → UsuarioInactivoException → 403
    └─ TokenProviderPort.generar()    → JwtTokenProvider.generar()
          claims: sub (email), uid, roles · expiración (JWT_EXPIRACION) · firma HMAC con JWT_SECRET
◄── 200 { token, tipo: "Bearer", expiraEn, usuario }
sesion.js guarda el token → api.js lo envía en cada petición: Authorization: Bearer <token>
```

**En las peticiones siguientes:** `JwtValidationFilter.doFilterInternal()` lee el header →
`JwtTokenProvider.validar()` verifica la firma y la expiración → crea `UsuarioAutenticado` y lo pone en el
`SecurityContext` → `@PreAuthorize("hasRole(...)")` decide por rol → el controlador recibe el usuario con
`@AuthenticationPrincipal` y llama `usuario.aSolicitante()`. Si alguien cambia el rol dentro del token, la firma ya no
coincide: `validar()` lo rechaza y `RestAuthenticationEntryPoint` responde 401.

## 2. Registro de cliente

```
ingresar.js ──POST /api/v1/auth/registro──► AuthController.registrar() (@Valid RegistroClienteRequest: email, clave ≥ 8 con letra y número)
  AutenticacionService.registrarCliente()
    ├─ delega en GestionClientesService.crear()   (el mismo caso de uso que usa el ADMIN para crear clientes)
    │     ├─ email o documento ya registrados → ConflictoException (409)
    │     ├─ PasswordEncoderPort → BCrypt (la clave nunca se guarda en texto plano)
    │     └─ Usuario + Cliente (dominio) → UsuarioRepository / ClienteRepository   ── tablas: usuarios, usuario_roles, clientes
    └─ emitirToken() → genera el JWT (queda logueado sin volver a escribir la clave)
◄── 201 { token, usuario }
```

Todo dentro de una transacción (`UseCaseConfig`): si falla el cliente, se revierte también el usuario.

## 3. Crear una reserva (el cliente solicita)

```
reserva.js ──POST /api/v1/reservas {fechaInicio, fechaFin, herramientas[], notas}──►
  ReservaController.crear()  (@PreAuthorize CLIENTE, @Valid CrearReservaRequest)
  GestionReservasService.crear()
    ├─ clienteIdDe(solicitante)                      ── tabla clientes
    ├─ cargarHerramientas() → HerramientaRepository.buscarPorIds()   ── tabla herramientas
    ├─ ReservaRepository.generarCodigo()  → RES-2026-0037
    ├─ Reserva.solicitar()  (DOMINIO)
    │     fechas no pasadas · máx. 90 días · un solo proveedor · sin herramientas repetidas · herramienta ACTIVA
    │     precio congelado en DetalleReserva · subtotal + IVA 19 % (Dinero) · estado PENDIENTE
    ├─ validarDisponibilidad()
    │     disponibles = unidadesOperativas − ReservaRepository.unidadesComprometidas()
    │     (SQL: SUM(cantidad) de reservas APROBADA/PAGADA/ENTREGADA que se cruzan con las fechas)
    │     no alcanza → ConflictoException (409)
    └─ ReservaRepository.guardar() → ReservaRepositoryAdapter   ── tablas: reservas, reserva_detalles
◄── 201 ReservaResult (ConsultaReservasPort, con cliente, proveedor y detalles)
```

**Antes de esto**, el carrito llama `POST /reservas/cotizacion` (público): mismo cálculo, sin guardar nada.

## 4. Aprobar una reserva (el proveedor)

```
panel (proveedor) ──PATCH /api/v1/reservas/{id}/aprobar──► ReservaController.aprobar() (@PreAuthorize PROVEEDOR/ADMIN)
  GestionReservasService.aprobar()
    ├─ validarEsProveedorDe()  → otro proveedor: AccesoDenegadoException (403)
    ├─ HerramientaRepository.bloquearParaReserva(ids)    SELECT … FOR UPDATE, ordenado por id (evita deadlocks)
    ├─ validarDisponibilidad() otra vez, ya con las filas bloqueadas
    └─ Reserva.aprobar(ahora, 24 h) → APROBADA, fechaLimitePago = ahora + 24 h
◄── 200 ReservaResult
```

**Por qué el bloqueo:** si dos proveedores (o dos pestañas) aprueban a la vez reservas que compiten por la última
unidad, la segunda transacción espera a que termine la primera y entonces ve que ya no hay unidades → 409.

**Vencimiento:** `VencimientoReservasJob` corre cada 5 min → `GestionReservasService.vencerReservasSinPago()` →
`Reserva.vencerSiCorresponde()` pasa a VENCIDA las APROBADAS sin pago (libera las unidades).

## 5. Pagar y facturar

```
panel (cliente) ──POST /api/v1/reservas/{id}/pagos {metodo, numeroTarjeta, titular}──►
  PagoController.pagar() (@PreAuthorize CLIENTE)
  GestionPagosService.pagar()
    1. ReservaRepository.bloquearPorId()     SELECT … FOR UPDATE → un doble clic no cobra dos veces
    2. ¿es del cliente? → si no, 403 (y no se cobra)
    3. validarMedioDePago() → tarjeta de 13 a 19 dígitos
    4. Reserva.verificarQueSePuedePagar()   APROBADA y dentro del plazo; si no, 422 SIN cobrar
    5. PasarelaPagoPort.cobrar() → PasarelaPagoSimuladaAdapter   (…0000 fondos, …1111 banco, resto aprobado)
    6. Pago.registrar() → PagoRepository.guardar()             ── tabla pagos (aprobado o rechazado; nunca la tarjeta)
    7. si aprobado:
         Reserva.confirmarPago() → PAGADA → ReservaRepository.guardar()      ── tabla reservas
         FacturaRepository.generarNumero() → FAC-2026-000030
         Factura.emitir() → FacturaRepository.guardar()                       ── tabla facturas
◄── 201 { aprobado, mensaje, pago: { …, reserva, factura } }
```

**PDF:** `GET /facturas/{id}/pdf` → `FacturaController` → `GestionPagosService.descargarFacturaPdf()` (valida que sea
del cliente, del proveedor emisor o del ADMIN) → `ConsultaFacturasPort` → `GeneradorFacturaPdfPort` →
`FacturaPdfAdapter` (OpenPDF) → bytes `application/pdf`. El frontend lo descarga con `fetch` + `Blob`, porque un
enlace normal no envía el header `Authorization`.

## 6. Entrega y devolución (con daños)

```
POST /api/v1/reservas/{id}/entrega {tipo, direccion, observaciones}
  DevolucionController → GestionDevolucionesService.registrarEntrega()
    ├─ ReservaRepository.bloquearPorId() · validarEsProveedorDe() · ya existe entrega → 409
    ├─ Reserva.registrarEntrega()  PAGADA → ENTREGADA
    └─ Entrega.registrar()  (DOMICILIO sin dirección → 422)        ── tablas: reservas, entregas

POST /api/v1/reservas/{id}/devolucion {estadoEquipo, observaciones, danos[]}
  GestionDevolucionesService.confirmarDevolucion()
    ├─ Reserva.registrarDevolucion()  ENTREGADA → DEVUELTA
    ├─ Devolucion.confirmar() → DevolucionRepository              ── tabla devoluciones
    └─ por cada daño: registrarDano()
          ReporteDano.reportar()  (equipo no BUENO, herramienta de la reserva)  ── tabla reportes_danos
          si GRAVE: bloquearParaReserva() + Herramienta.sacarDeServicio(1)     ── herramientas.unidades_fuera_servicio + 1

PATCH /api/v1/devoluciones/{id}/validar   (solo ADMIN) → Devolucion.validar() una sola vez
PATCH /api/v1/danos/{id}/estado           (solo ADMIN) → ReporteDano.avanzarA()  ABIERTO → EN_REVISION → RESUELTO
      al RESOLVER un GRAVE: Herramienta.reintegrarAServicio(1)
```

## 7. Un reporte (ej. resumen del panel)

```
tablero.js ──GET /api/v1/reportes/panel?desde=&hasta=──► ReporteController (@PreAuthorize ADMIN/PROVEEDOR)
  ReportesService.consultarPanel()
    ├─ alcance(): ADMIN → el proveedor que pida o todos · PROVEEDOR → SIEMPRE el suyo · CLIENTE → 403
    ├─ periodo(): por defecto 1 de enero → hoy (Clock); desde > hasta → 422
    ├─ ConsultaReportesPort → ConsultaReportesAdapter (SQL nativo, NamedParameterJdbcTemplate)
    │     ingresos: SUM(pagos.monto) de pagos APROBADOS por fecha de pago
    │     más alquiladas: COUNT/SUM sobre reserva_detalles de reservas PAGADA/ENTREGADA/DEVUELTA, GROUP BY herramienta
    └─ indicadores derivados en Java: ticket promedio, tasa de aprobación, margen, ocupación, alertas
◄── 200 { resumen, ingresosPorMes, masAlquiladas, clientesFrecuentes }
graficas.js dibuja las columnas y barras en SVG.
```

## 8. Un error cualquiera

```
Caso de uso lanza ReglaDeNegocioException("No se puede pagar… PENDIENTE")
  → sale del proxy transaccional (rollback)
  → GlobalExceptionHandler.manejarReglaDeNegocio() → ProblemDetail 422 {type, title, status, detail, instance, timestamp}
  → api.js lo convierte en ApiError → ui.js lo muestra en un toast o dentro del formulario (errores por campo si es 400)
```
