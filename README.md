# 🛠️ Plataforma de Renta de Herramientas y Equipos de Construcción

API REST construida con **Spring Boot** para que proveedores publiquen su inventario de herramientas y equipos de construcción, y que los clientes puedan reservarlos, pagarlos y descargar su factura. Incluye panel por rol (Administrador, Proveedor, Cliente), control de disponibilidad, gestión de devoluciones y daños, y reportes de rentabilidad.

> Proyecto principal del skill de Spring Boot — Campuslands.
> Autor: **Sebastián Acelas**

---

## 📑 Tabla de contenidos

1. [Tecnologías](#-tecnologías)
2. [Arquitectura](#-arquitectura)
3. [Estructura del proyecto](#-estructura-del-proyecto)
4. [Requisitos previos](#-requisitos-previos)
5. [Instalación y ejecución](#-instalación-y-ejecución)
6. [Configuración (`application.yml`)](#-configuración)
7. [Base de datos](#-base-de-datos)
8. [Autenticación y roles](#-autenticación-y-roles)
9. [Endpoints de la API](#-endpoints-de-la-api)
10. [Flujo de trabajo con Git Flow](#-flujo-de-trabajo-con-git-flow)
11. [Estado del proyecto](#-estado-del-proyecto)

---

## 🧰 Tecnologías

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 4.1.1 |
| Servidor | Apache Tomcat (embebido) | incluido en Spring Boot |
| Persistencia | Spring Data JPA + Hibernate | gestionado por Spring Boot |
| Base de datos | MySQL | 8.x |
| Migraciones | Flyway | gestionado por Spring Boot |
| Seguridad | Spring Security + JWT (jjwt) | 7.x / 0.13.0 |
| Documentación API | springdoc-openapi (Swagger UI) | 3.1.0 |
| Pruebas | JUnit 5, AssertJ, Mockito | gestionado por Spring Boot |
| Build | Maven (multimódulo) | 3.9+ |

---

## 🏛️ Arquitectura

El proyecto aplica **Arquitectura Hexagonal (Puertos y Adaptadores)** con **Domain-Driven Design**, dividido en tres módulos Maven. La regla central es que **las dependencias apuntan hacia adentro**: la infraestructura conoce al dominio, pero el dominio no conoce a nadie.

```
infrastructure  ──►  application  ──►  domain
 (Spring, JPA,        (casos de uso,      (reglas de negocio,
  REST, MySQL)         puertos)            Java puro)
```

### Equivalencia con la arquitectura por capas

El enunciado pide separación en Controllers, Services, Repositories, DTOs y Entities. En hexagonal esas capas existen, con un rol más preciso:

| Capa clásica | Dónde vive en este proyecto | Módulo |
|---|---|---|
| Controllers | `adapters/inbound/controller` | infrastructure |
| Services | `application/usecases` (implementan los puertos de entrada) | application |
| Repositories | Interfaz en `domain/repositories`, implementación JPA en `adapters/outbound/persistence/repositories` | domain + infrastructure |
| DTOs | HTTP en `adapters/inbound/dtos`, de aplicación en `application/dtos` | infrastructure + application |
| Entities | Modelo de negocio en `domain/model`, tablas en `adapters/outbound/persistence/entities` | domain + infrastructure |

**¿Por qué separar el modelo de dominio de las entidades JPA?** Porque así las reglas del negocio (por ejemplo, "no se puede reservar una herramienta sin unidades disponibles") se prueban sin base de datos, y cambiar MySQL por otro motor no toca el dominio.

---

## 🗂️ Estructura del proyecto

```
Proyecto_RentaHerramientas_AcelasSebastian
├── pom.xml                         # Proyecto padre (packaging pom)
├── database/                       # Script de creación de BD y usuario
├── docs/                           # Diagrama relacional
├── domain/                         # Núcleo del negocio (Java puro)
│   └── src/main/java/com/rentaherramientas/domain
│       ├── model
│       │   ├── aggregates
│       │   └── valueobject
│       ├── repositories            # Puertos de persistencia
│       ├── exceptions
│       └── events
├── application/                    # Casos de uso (Java puro)
│   └── src/main/java/com/rentaherramientas/application
│       ├── config
│       ├── dtos
│       ├── exceptions
│       ├── mappers
│       ├── ports                   # in: casos de uso / out: pagos, archivos...
│       └── usecases
└── infrastructure/                 # Spring Boot: adaptadores y arranque
    └── src/main/java/com/rentaherramientas/infrastructure
        ├── RentaHerramientasApplication.java
        ├── config                  # Wiring de casos de uso, Swagger
        ├── security                # JWT, filtros, CORS, BCrypt
        └── adapters
            ├── inbound
            │   ├── controller
            │   ├── exceptionhandler
            │   └── dtos
            └── outbound
                └── persistence
                    ├── entities
                    ├── mappers
                    └── repositories
```

---

## ✅ Requisitos previos

- JDK 21 (`java -version`)
- Maven 3.9 o superior (`mvn -v`)
- MySQL 8 en ejecución
- Git

---

## 🚀 Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/sebastianace/Proyecto_RentaHerramientas_AcelasSebastian.git
cd Proyecto_RentaHerramientas_AcelasSebastian
```

### 2. Crear la base de datos y el usuario

Abre `database/00_create_database_and_user.sql`, cambia la clave `cambia_esta_clave` por una tuya y ejecútalo con el usuario `root` (MySQL Workbench o consola):

```bash
mysql -u root -p < database/00_create_database_and_user.sql
```

Las tablas **no** se crean a mano: Flyway las crea automáticamente al arrancar la aplicación.

### 3. Configurar las variables de entorno

Copia la plantilla y escribe tus valores reales:

```bash
# Windows (PowerShell)
Copy-Item .env.example .env
# Linux / macOS
cp .env.example .env
```

Genera tu propia clave para firmar los JWT (48 bytes aleatorios en Base64) y pégala en `JWT_SECRET`:

```powershell
# Windows (PowerShell)
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

```bash
# Linux / macOS
openssl rand -base64 48
```

### 4. Compilar e instalar los módulos

```bash
mvn clean install
```

### 5. Ejecutar

```bash
mvn -pl infrastructure spring-boot:run
```

La API queda disponible en `http://localhost:8080` y la documentación interactiva en **`http://localhost:8080/swagger-ui.html`**.

---

## ⚙️ Configuración

La configuración está en `infrastructure/src/main/resources` y se divide por perfiles:

| Archivo | Uso |
|---|---|
| `application.yml` | Configuración común: datasource, JPA, Flyway, Tomcat, carga de archivos |
| `application-dev.yml` | Desarrollo: SQL visible en consola, logs en DEBUG, carga datos de prueba |
| `application-prod.yml` | Producción: sin SQL en consola, `DB_URL` obligatoria |

Ejemplo (fragmento de `application.yml`):

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate   # Flyway maneja el esquema
  flyway:
    locations: classpath:db/migration
```

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/renta_herramientas` |
| `DB_USERNAME` | Usuario de la BD | `renta_app` |
| `DB_PASSWORD` | Clave de la BD | — |
| `MEDIA_BASE_PATH` | Carpeta para imágenes | `./uploads` |
| `JWT_SECRET` | Clave Base64 (mínimo 32 bytes) para firmar los tokens | — |
| `JWT_EXPIRACION` | Vida del token | `60m` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos | `http://localhost:5500` |

---

## 🗄️ Base de datos

### Diagrama relacional

![Diagrama relacional](docs/diagrama-relacional.png)

El modelo tiene **15 tablas** organizadas por contexto de negocio:

| Contexto | Tablas |
|---|---|
| Identidad | `roles`, `usuarios`, `usuario_roles`, `proveedores`, `clientes` |
| Catálogo | `categorias`, `herramientas`, `herramienta_imagenes` |
| Alquileres | `reservas`, `reserva_detalles`, `entregas`, `devoluciones`, `reportes_danos` |
| Facturación | `pagos`, `facturas` |

**Reglas de diseño principales**

- Una reserva pertenece a **un solo proveedor**, que es quien la aprueba o rechaza.
- El precio por día se **congela** en `reserva_detalles` al reservar; los cambios de precio posteriores no afectan reservas ni facturas existentes.
- **Disponibilidad** de una herramienta en un rango de fechas = `stock_total` − `unidades_fuera_servicio` − unidades en reservas `APROBADA`, `PAGADA` o `ENTREGADA` que se cruzan con ese rango.
- Días cobrados = `fecha_fin − fecha_inicio + 1` (ambos días incluidos). IVA del 19 %.
- Los montos usan `DECIMAL(12,2)`; los estados son `VARCHAR` con restricción `CHECK`.
- Usuarios y herramientas con historial no se eliminan físicamente: se desactivan (borrado lógico).

**Ciclo de vida de una reserva**

```
PENDIENTE ──► APROBADA ──► PAGADA ──► ENTREGADA ──► DEVUELTA
    │             │
    ├► RECHAZADA  ├► VENCIDA (no se pagó antes de fecha_limite_pago)
    └► CANCELADA  └► CANCELADA
```

### Scripts SQL

El esquema y los datos se crean con **Flyway** al arrancar la aplicación. Los scripts están en `infrastructure/src/main/resources/db/`:

| Script | Contenido |
|---|---|
| `migration/V1__create_identity_tables.sql` | Usuarios, roles, proveedores y clientes |
| `migration/V2__create_catalog_tables.sql` | Categorías, herramientas e imágenes |
| `migration/V3__create_rental_tables.sql` | Reservas, detalles, entregas, devoluciones y daños |
| `migration/V4__create_billing_tables.sql` | Pagos y facturas |
| `migration/V5__add_query_indexes.sql` | Índices para búsquedas y reportes |
| `seed/V6__seed_identity.sql` | 42 usuarios (2 admin, 20 proveedores, 20 clientes) |
| `seed/V7__seed_catalog.sql` | 20 categorías, 40 herramientas y sus imágenes |
| `seed/V8__seed_rentals.sql` | 35 reservas en todos los estados, entregas, devoluciones y daños |
| `seed/V9__seed_billing.sql` | 30 pagos (aprobados y rechazados) y 27 facturas |

Los datos de prueba (`db/seed`) **solo se cargan en el perfil `dev`**. En `prod` Flyway aplica únicamente `db/migration`.

Para crear la base sin la aplicación, ejecuta los scripts en orden (V1 → V9) sobre la base `renta_herramientas`.

> La tabla `roles` es un catálogo fijo de 3 registros (ADMIN, PROVEEDOR, CLIENTE). Todas las demás tablas tienen al menos 20 registros.

### Usuarios de prueba

Todos usan la clave **`Renta2026*`** (almacenada como hash BCrypt).

| Rol | Email |
|---|---|
| ADMIN | `admin@rentaherramientas.com` |
| PROVEEDOR | `carlos.rueda@herroriente.com` |
| CLIENTE | `juanpablo.ardila@gmail.com` |

---

## 🔐 Autenticación y roles

La API es **stateless**: no guarda sesiones. Cada petición protegida debe enviar un **JWT** en el header:

```
Authorization: Bearer <token>
```

### Flujo

```
1. POST /api/v1/auth/login  {email, password}
2. Spring Security verifica la clave con BCrypt (AuthenticationManager + UserDetailsService)
3. La API responde {token, tipo: "Bearer", expiraEn, usuario}
4. El cliente envía el token en cada petición
5. JwtValidationFilter valida firma, emisor y expiración → registra al usuario en el SecurityContext
6. @PreAuthorize decide si su rol puede usar el endpoint
```

El token está firmado con **HMAC-SHA** usando `JWT_SECRET`. Su contenido (*payload*) lleva el email (`sub`), el id del usuario (`uid`), sus `roles`, el emisor (`iss`) y la expiración (`exp`). Si alguien modifica el payload, la firma deja de coincidir y el token se rechaza.

### Roles

| Rol | Cómo se crea | Qué puede hacer |
|---|---|---|
| `ADMIN` | Datos iniciales | Gestiona clientes y proveedores, supervisa alquileres, pagos y reportes |
| `PROVEEDOR` | Lo registra el ADMIN | Gestiona su inventario, sus reservas y su facturación |
| `CLIENTE` | Registro público (`/auth/registro`) o el ADMIN | Explora herramientas, reserva, paga y consulta su historial |

Además del rol, los casos de uso validan la **propiedad del recurso**: un cliente solo puede ver y editar su propio perfil, aunque conozca el id de otro.

### Respuestas de error

Todos los errores siguen el estándar **ProblemDetail (RFC 9457)**:

```json
{
  "type": "about:blank",
  "title": "Conflicto",
  "status": 409,
  "detail": "Ya existe un usuario registrado con el email laura.perez@gmail.com",
  "instance": "/api/v1/auth/registro",
  "timestamp": "2026-10-14T16:00:00Z"
}
```

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos (incluye el detalle de cada campo en `errores`) |
| 401 | Sin token, token vencido o credenciales incorrectas |
| 403 | Rol insuficiente, recurso de otro usuario o usuario inactivo |
| 404 | El recurso no existe |
| 409 | Dato duplicado (email, documento, NIT) |
| 422 | Regla de negocio incumplida |

---

## 📡 Endpoints de la API

La documentación completa, con ejemplos y la opción de probar cada endpoint, está en **Swagger UI**: `http://localhost:8080/swagger-ui.html`.

### Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Público | Iniciar sesión |
| POST | `/api/v1/auth/registro` | Público | Registrarse como cliente |
| GET | `/api/v1/auth/me` | Autenticado | Perfil del usuario autenticado |

Ejemplo de login:

```json
POST /api/v1/auth/login
{
  "email": "admin@rentaherramientas.com",
  "password": "Renta2026*"
}
```

```json
200 OK
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tipo": "Bearer",
  "expiraEn": "2026-10-14T17:00:00Z",
  "usuario": {
    "id": 1,
    "email": "admin@rentaherramientas.com",
    "nombres": "Administrador",
    "apellidos": "Principal",
    "telefono": "3001112233",
    "activo": true,
    "roles": ["ADMIN"]
  }
}
```

### Clientes

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/v1/clientes?pagina=0&tamanio=20` | ADMIN | Listar clientes |
| GET | `/api/v1/clientes/{id}` | ADMIN, CLIENTE (propio) | Obtener un cliente |
| POST | `/api/v1/clientes` | ADMIN | Registrar un cliente |
| PUT | `/api/v1/clientes/{id}` | ADMIN, CLIENTE (propio) | Actualizar un cliente |
| DELETE | `/api/v1/clientes/{id}` | ADMIN | Desactivar (borrado lógico) |
| PATCH | `/api/v1/clientes/{id}/activar` | ADMIN | Reactivar |

### Proveedores

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/v1/proveedores?pagina=0&tamanio=20` | ADMIN | Listar proveedores |
| GET | `/api/v1/proveedores/{id}` | ADMIN, PROVEEDOR (propio) | Obtener un proveedor |
| POST | `/api/v1/proveedores` | ADMIN | Registrar un proveedor |
| PUT | `/api/v1/proveedores/{id}` | ADMIN, PROVEEDOR (propio) | Actualizar un proveedor |
| DELETE | `/api/v1/proveedores/{id}` | ADMIN | Desactivar (borrado lógico) |
| PATCH | `/api/v1/proveedores/{id}/activar` | ADMIN | Reactivar |

---

## 🌿 Flujo de trabajo con Git Flow

| Rama | Propósito |
|---|---|
| `main` | Código estable y entregado. Solo recibe merges desde `release/*` o `hotfix/*` |
| `develop` | Integración de funcionalidades terminadas |
| `feature/*` | Una funcionalidad por rama, sale de `develop` y vuelve a `develop` por Pull Request |
| `release/*` | Preparación de una versión (README final, ajustes) antes de pasar a `main` |
| `hotfix/*` | Correcciones urgentes sobre `main` |

Los Pull Requests se integran con *merge commit* (`--no-ff`) para conservar el historial de cada rama.

### Convención de commits

Se usa [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/): `tipo(alcance): descripción`

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de un error |
| `docs` | Documentación |
| `build` | Dependencias o configuración de Maven |
| `chore` | Tareas de mantenimiento y configuración |
| `refactor` | Cambio interno sin alterar el comportamiento |
| `test` | Pruebas |

Ejemplo: `feat(reservas): validar disponibilidad antes de aprobar una reserva`

---

## 📌 Estado del proyecto

- [x] Fase 0 — Estructura multimódulo, perfiles y configuración base
- [x] Fase 1 — Modelo relacional, migraciones Flyway y datos de prueba
- [x] Fase 2 — Usuarios, roles, Spring Security y JWT
- [ ] Fase 3 — Catálogo de herramientas e imágenes
- [ ] Fase 4 — Reservas y control de disponibilidad
- [ ] Fase 5 — Pagos y facturación
- [ ] Fase 6 — Entregas, devoluciones y reportes de daños
- [ ] Fase 7 — Reportes y estadísticas
- [ ] Fase 8 — Swagger, colección de Postman y pruebas
- [ ] Fase 9 — Frontend
