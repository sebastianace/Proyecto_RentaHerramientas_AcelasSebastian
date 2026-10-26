# Ejecutar ObraYa con Docker

Con **Docker Compose** se levantan la base de datos (MySQL 8), el backend (Spring Boot) y el frontend (nginx) con un solo
comando, sin instalar Java, Maven ni MySQL.

```
 navegador ──► frontend (nginx, :5500) ──┐
     │                                   │ (el navegador llama a la API)
     └──────► backend (Spring Boot, :8080) ──► db (MySQL 8.0, red interna)
```

## Requisitos

- Docker con el plugin Compose v2: `docker --version` y `docker compose version`.
- Un archivo `.env` en la raíz con, como mínimo, `DB_PASSWORD` y `JWT_SECRET` (ver el README principal).

## Levantar el proyecto

```bash
docker compose up --build
```

La primera vez descarga imágenes y compila el proyecto (varios minutos). Cuando termine:

| Qué | Dirección |
|---|---|
| Tienda ObraYa | http://localhost:5500 |
| Swagger | http://localhost:8080/swagger-ui.html |

Usuarios de prueba (clave `Renta2026*`): `juanpablo.ardila@gmail.com` (cliente), `carlos.rueda@herroriente.com`
(proveedor) y `admin@rentaherramientas.com` (administrador).

## Comandos útiles

| Comando | Qué hace |
|---|---|
| `docker compose up -d --build` | Levanta todo en segundo plano |
| `docker compose ps` | Estado de los 3 servicios |
| `docker compose logs -f backend` | Sigue los logs del backend |
| `docker compose down` | Detiene y quita los contenedores, **conserva** la base de datos y las imágenes subidas |
| `docker compose down -v` | Lo anterior y **borra** también la base de datos y las imágenes subidas |
| `docker compose up -d --build backend` | Reconstruye solo el backend tras cambiar código |

## Cómo funciona

- **db:** `mysql:8.0`. Las variables `MYSQL_DATABASE`, `MYSQL_USER` y `MYSQL_PASSWORD` crean la base y el usuario de la app
  la primera vez. Las tablas las crea Flyway cuando arranca el backend. No se expone al computador.
- **backend:** imagen de dos etapas (`docker/backend.Dockerfile`): Maven + JDK 21 compilan; la imagen final solo lleva el
  JRE 21 y el `.jar`, y corre con un usuario sin privilegios. Espera a que MySQL esté *healthy* antes de arrancar.
- **frontend:** nginx sirve la carpeta `frontend/`. El navegador llama a la API en `http://localhost:8080`.
- **Volúmenes:** `mysql_data` (la base de datos) y `uploads` (fotos de herramientas).
- **Secretos:** el `.env` está en `.dockerignore`, así que nunca queda dentro de una imagen.
- **Perfil `dev`:** carga los datos de prueba (`db/seed`). No uses el perfil `prod` sin antes tener los roles en una
  migración, porque hoy se insertan en el seed.

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Falta DB_PASSWORD en el archivo .env` | Crea el `.env` (`cp .env.example .env`) y completa `DB_PASSWORD` y `JWT_SECRET` |
| `Access denied for user 'renta_app'` en el backend | El volumen ya existía con otra clave. `docker compose down -v` y vuelve a levantar |
| `permission denied ... docker.sock` | Usa `sudo docker compose ...` o agrega tu usuario al grupo `docker` |
| `the --mount option requires BuildKit` | Ejecuta con `DOCKER_BUILDKIT=1 docker compose up --build` |
| `port is already allocated` en el 8080 | Otro programa usa ese puerto. Agrega `BACKEND_PORT=8081` al `.env`, levanta de nuevo y abre `http://localhost:5500/?api=http://localhost:8081` (Swagger queda en `http://localhost:8081/swagger-ui.html`) |
| `port is already allocated` en el 5500 | Cierra el programa que lo usa, o cambia el `5500` de la izquierda en `ports` del frontend y agrega ese origen a `CORS_ALLOWED_ORIGINS` |
| El frontend carga pero no muestra datos | El backend aún arranca (tarda ~30 s) o el navegador guardó otra API: abre `http://localhost:5500/?api=http://localhost:8080` |
