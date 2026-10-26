# syntax=docker/dockerfile:1
# =====================================================================
# Imagen del BACKEND (Spring Boot). Se construye en dos etapas:
#   1) "build": una imagen con Maven y JDK 21 compila los 3 módulos y arma el .jar ejecutable.
#   2) "runtime": una imagen pequeña con solo el JRE 21 y el .jar. Sin Maven, sin código fuente.
# El contexto de construcción es la raíz del repositorio (el proyecto es multimódulo).
# =====================================================================

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY domain domain
COPY application application
COPY infrastructure infrastructure

# La caché de ~/.m2 hace que, después de la primera vez, Maven no vuelva a descargar las dependencias.
# -DskipTests: las pruebas se corren aparte (mvn test); aquí solo se arma el .jar.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios: si alguien vulnera la app, no entra al contenedor como root.
# /app/uploads es donde se guardan las imágenes de las herramientas (se monta como volumen).
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
 && mkdir -p /app/uploads \
 && chown -R app:app /app

# El .jar "gordo" de Spring Boot (lleva dentro Tomcat y todas las dependencias).
COPY --from=build --chown=app:app /build/infrastructure/target/infrastructure-*.jar app.jar

USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Bogota"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
