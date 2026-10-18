# ObraYa — Frontend

Tienda virtual para alquilar herramientas y equipos de construcción. Está hecha con **HTML, CSS y JavaScript puro**
(módulos ES, sin frameworks ni compilación) y consume la API REST del backend con **JWT**.

## Cómo ejecutarlo

1. Arranca el backend (`mvn -pl infrastructure spring-boot:run`, queda en `http://localhost:8080`).
2. Abre la carpeta `frontend/` con **Live Server** de VS Code (puerto **5500**, que es el origen permitido por CORS).
   También sirve `python -m http.server 5500` dentro de `frontend/`.
3. Entra a `http://127.0.0.1:5500/index.html`.

Si el backend corre en otro puerto, abre cualquier página con `?api=http://localhost:8081`: la URL queda guardada
en el navegador.

**Usuarios de prueba** (clave `Renta2026*`), también como botones en la pantalla de ingreso:

| Rol | Correo |
|---|---|
| CLIENTE | `juanpablo.ardila@gmail.com` |
| PROVEEDOR | `carlos.rueda@herroriente.com` |
| ADMIN | `admin@rentaherramientas.com` |

**Tarjetas de la pasarela simulada:** `4242 4242 4242 4242` aprobada · terminada en `0000` rechazada por fondos ·
terminada en `1111` rechazada por el banco. PSE y Nequi siempre se aprueban.

## Páginas

| Página | Qué hace |
|---|---|
| `index.html` | Tienda: buscador, categorías, catálogo con filtros (texto, categoría, ciudad, precio máximo, fechas) y paginación. Con fechas, cada tarjeta muestra las unidades libres reales |
| `herramienta.html?id=` | Detalle: galería, ficha técnica, **calendario de disponibilidad** de 5 semanas, cantidad y **cotización en vivo** |
| `reserva.html` | Checkout: carrito, fechas, notas y total calculado por el servidor (`POST /reservas/cotizacion`) |
| `ingresar.html` | Ingreso y registro de clientes |
| `panel.html` | Panel según el rol (ver abajo) |

**Panel del CLIENTE:** resumen, mis reservas (línea de tiempo, pagar, cancelar, descargar factura), pagos, facturas y perfil.
**Panel del PROVEEDOR:** mi negocio (reportes), solicitudes por aprobar, reservas (registrar entrega y devolución con
daños), inventario (crear, editar, estado y fotos), devoluciones, daños, facturación, pagos y datos de la empresa.
**Panel del ADMIN:** tablero de la plataforma (ingresos, rentabilidad, alertas), usuarios, alquileres, pagos,
facturación, devoluciones por validar, incidencias, herramientas y categorías.

## Organización del código

```
frontend/
├── *.html                    Una página por pantalla; solo traen la estructura base
└── assets/
    ├── css/app.css           Sistema de diseño: tokens (colores, radios, sombras), tema claro/oscuro y componentes
    └── js/
        ├── config.js         URL de la API, usuarios y tarjetas de prueba
        ├── api.js            Cliente HTTP: agrega el JWT, convierte ProblemDetail en ApiError, maneja el 401
        ├── sesion.js         Guarda el token y el usuario en localStorage
        ├── carrito.js        Carrito en localStorage (una reserva = un solo proveedor)
        ├── iconos.js         Sprite SVG: íconos e ilustraciones de cada categoría
        ├── ui.js             Formato, escape de HTML, modales, notificaciones, layout común y carrito lateral
        ├── graficas.js       Gráficas SVG de una serie (columnas y barras) con tooltip y vista de tabla
        ├── paginas/          Un módulo por página (tienda, herramienta, reserva, ingresar, panel)
        └── secciones/        Secciones del panel: comunes, cliente, proveedor, admin, tablero, devoluciones
```

## Decisiones (para explicar en la sustentación)

- **JavaScript puro con módulos ES.** Cada archivo importa solo lo que usa; no hace falta compilar.
- **Todo dato de la API pasa por `esc()` antes de ir al HTML.** Evita XSS: si alguien registra una herramienta con
  `<script>` en el nombre, se muestra como texto.
- **El rol en el navegador solo decide qué pantalla mostrar.** Quien autoriza es el backend, que valida la firma del
  JWT en cada petición: cambiar el rol en `localStorage` no da acceso a nada.
- **Sesión vencida:** si la API responde 401 con un token, `api.js` cierra la sesión y lleva al ingreso, recordando a
  dónde volver.
- **Las descargas de PDF usan `fetch` + `Blob`**, porque un enlace normal no enviaría el encabezado `Authorization`.
- **Ilustraciones vectoriales propias por categoría.** Las imágenes de los datos de prueba son placeholders con
  texto; se reemplazan por dibujos que respetan el tema. Las fotos reales que suben los proveedores sí se muestran.
- **Tema claro y oscuro** con los mismos componentes: solo cambian los tokens de color.
- **Cada sección del panel recibe un contenedor nuevo**, así sus eventos se descartan al navegar y una acción nunca se
  ejecuta dos veces.
- **Accesible y responsivo:** navegación por teclado, `aria-*` en modales y pestañas, contraste validado en las
  gráficas, y diseño para móvil, tableta y escritorio.
