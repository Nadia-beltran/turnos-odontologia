# Proposal

## Why

El repositorio solo tiene la documentación de la fundación: no hay proyecto backend sobre el cual trabajar. C-01 es el primer paso del camino crítico (`CHANGES.md`). Sin una base común (estructura de módulos, migraciones, formato de errores, base de tests e integración continua), los cinco integrantes divergirían desde la primera ficha, y C-03 y C-04 no pueden empezar.

## What Changes

- Se crea `backend/`: proyecto Gradle con **Spring Boot 4.2.0**, generado desde Spring Initializr e incluyendo el *wrapper* (`gradlew`). Dependencias: Web, Security, Data JPA (Hibernate 7), Validation, Mail, Flyway, driver de PostgreSQL y Testcontainers.
- **Java 17** en lugar de Java 21, y **Spring Boot 4.2** en lugar de 3.x (decisiones de la usuaria del 2026-11-28; Initializr ya no ofrece 3.x). Se actualizan el supuesto SU-01, la tabla de stack de `knowledge-base/02`, `CLAUDE.md` y la ficha C-01 de `CHANGES.md`.
- Estructura de paquetes por módulo (`ar.turnos.{shared,auth,consultorio,agenda,turnos,listaespera,pacientes,clinica,notificaciones,auditoria,reportes,admin}`), según `knowledge-base/08`.
- Perfiles `dev` y `prod`, y un `application.yml` que lee las variables de entorno de `knowledge-base/08 §Variables de entorno`.
- Migración Flyway `V1__extensiones.sql` que habilita `btree_gist`. Hibernate queda con `ddl-auto: validate` (DD-13).
- Manejo global de errores de la API en formato `ProblemDetail` (RFC 9457).
- Base reutilizable de tests de integración con Testcontainers (PostgreSQL 15).
- `docker-compose.yml` con PostgreSQL 15 y Mailpit para desarrollo local.
- Workflow de GitHub Actions `backend.yml`: build y tests en cada PR y push a `main`.
- `.gitattributes` con `eol=lf`, para que el equipo use los mismos finales de línea.
- Configuración inicial de Spring Security: todo autenticado salvo lo que se habilite explícitamente. No incluye login, que llega en C-06.

**Limitación conocida**: Docker no está instalado en la máquina de la responsable. Por ahora, el test con Testcontainers y `docker compose` se validan solo en CI. En local se verifican la compilación y los tests unitarios.

## Capabilities

### New Capabilities
- `api-errors`: formato uniforme de las respuestas de error de la API REST (RFC 9457 `application/problem+json`), del que depende el cliente del frontend.
- `schema-migrations`: versionado del esquema de PostgreSQL con Flyway al arrancar el backend, validación del modelo de Hibernate contra el esquema y extensiones requeridas.

### Modified Capabilities
<!-- Ninguna: no existen specs previas en openspec/specs/. -->

## Impact

- **Código nuevo**: `backend/` completo, `docker-compose.yml`, `.github/workflows/backend.yml`, `.gitattributes`.
- **Documentación**: `knowledge-base/02_descripcion_general.md` (stack), `knowledge-base/09_decisiones_y_supuestos.md` (SU-01), `CLAUDE.md` (stack), `CHANGES.md` (ficha C-01: Java 17).
- **Dependencias**: Spring Boot 4.2.0 (Jakarta EE 11, Hibernate 7), Flyway, driver PostgreSQL, Testcontainers. La descarga del ZIP de Spring Initializr está aprobada por la usuaria.
- **Equipo**: todos deben usar JDK 17. CI usa Temurin 17.
- **Desbloquea**: C-03 (junto con C-02) y C-04.
