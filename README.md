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
7. [Flujo de trabajo con Git Flow](#-flujo-de-trabajo-con-git-flow)
8. [Estado del proyecto](#-estado-del-proyecto)

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
├── database/                       # Scripts SQL de apoyo
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
        ├── config
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

### 4. Compilar e instalar los módulos

```bash
mvn clean install
```

### 5. Ejecutar

```bash
mvn -pl infrastructure spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

---

## ⚙️ Configuración

La configuración está en `infrastructure/src/main/resources` y se divide por perfiles:

| Archivo | Uso |
|---|---|
| `application.yml` | Configuración común: datasource, JPA, Flyway, Tomcat, carga de archivos |
| `application-dev.yml` | Desarrollo: SQL visible en consola, logs en DEBUG |
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
- [ ] Fase 1 — Modelo relacional, migraciones Flyway y datos de prueba
- [ ] Fase 2 — Usuarios, roles, Spring Security y JWT
- [ ] Fase 3 — Catálogo de herramientas e imágenes
- [ ] Fase 4 — Reservas y control de disponibilidad
- [ ] Fase 5 — Pagos y facturación
- [ ] Fase 6 — Entregas, devoluciones y reportes de daños
- [ ] Fase 7 — Reportes y estadísticas
- [ ] Fase 8 — Swagger, colección de Postman y pruebas
- [ ] Fase 9 — Frontend
