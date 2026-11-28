# Backend: Estudio Sonrisa

API REST en Spring Boot 4.2 con Java 17, Gradle (wrapper), Hibernate, Flyway y PostgreSQL. Decisiones: `knowledge-base/09_decisiones_y_supuestos.md` (DD-13, DD-15). Arquitectura y módulos: `knowledge-base/08_arquitectura_propuesta.md`.

## Requisitos

- **JDK 17** (por ejemplo, Temurin 17). No hace falta instalar Gradle: el wrapper lo descarga.
- **Docker**, solo para los tests de integración y para levantar PostgreSQL y Mailpit en local. Sin Docker se puede compilar y correr los tests unitarios.

## Comandos

Todos se ejecutan desde `backend/`. En Windows (PowerShell o cmd) usá `.\gradlew.bat` en lugar de `./gradlew`.

| Qué | Comando |
|---|---|
| Compilar | `./gradlew compileJava` |
| Tests (los de integración se omiten si no hay Docker) | `./gradlew test` |
| Build completo, como en CI | `./gradlew build` |
| Levantar PostgreSQL 15 y Mailpit (desde la raíz del repo) | `docker compose up -d` |
| Correr el backend en local (perfil `dev`) | `./gradlew bootRun` |
| Correr el backend con PostgreSQL de Testcontainers | `./gradlew bootTestRun` |

En el perfil `dev` la configuración tiene valores por defecto que coinciden con `docker-compose.yml`: base `turnos`, usuario `turnos` y SMTP en `localhost:1025`. Los emails se ven en http://localhost:8025.

## Configuración

`src/main/resources/application.yml` lee variables de entorno (lista completa en `knowledge-base/08 §Variables de entorno`).

- **`dev`** (perfil por defecto): todas las variables tienen valor por defecto.
- **`prod`**: no hay valores por defecto. Si falta alguna variable obligatoria, el backend no arranca y el error lista exactamente cuáles faltan (`ar.turnos.shared.config.RequiredEnvironment`).

Nunca guardes secretos en el repositorio: en producción se cargan en el panel del hosting.

## Convenciones

- **Módulos**: un paquete por dominio bajo `ar.turnos` (`shared`, `auth`, `consultorio`, `agenda`, `turnos`, `listaespera`, `pacientes`, `clinica`, `notificaciones`, `auditoria`, `reportes`, `admin`). Cada `package-info.java` describe su responsabilidad.
- **Migraciones**: `src/main/resources/db/migration/V<N>__descripcion.sql`, con el número reservado en `CHANGES.md` para cada ficha. Nunca edites una migración ya fusionada: Flyway detecta el cambio de checksum y el backend no arranca. Hibernate solo valida el esquema (`ddl-auto: validate`).
- **Errores**: toda respuesta de error es `application/problem+json` (RFC 9457), a cargo de `ar.turnos.shared.web.ApiExceptionHandler`. Las excepciones no previstas devuelven 500 con un mensaje genérico; el detalle técnico va solo al log.
- **Seguridad**: por defecto toda ruta requiere sesión (`ar.turnos.shared.security.SecurityConfig`). Cada ficha abre explícitamente las rutas públicas que necesita.
- **Tests de integración**: extendé `ar.turnos.shared.AbstractIntegrationTest`, que levanta PostgreSQL 15 real con Testcontainers. Nunca uses H2 ni mocks de la base.

## CI

`.github/workflows/backend.yml` corre `./gradlew build` con Temurin 17 en cada PR y en cada push a `main` que toque `backend/`. En CI Docker es obligatorio, así que los tests de integración siempre se ejecutan ahí.
