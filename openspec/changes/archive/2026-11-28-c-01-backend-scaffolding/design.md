# Design

## Context

El repositorio no tiene código: solo la documentación de la fundación. La motivación está en `proposal.md`. El entorno de la responsable condiciona el enfoque:

- JDK 17 instalado.
- Sin Gradle ni Docker.
- Node disponible.
- Sin Python.

Spring Initializr (consultado el 2026-11-28) solo ofrece Spring Boot 4.x, con 4.2.0 como versión estable por defecto. La usuaria eligió 4.2.0 con Java 17.

Las restricciones vienen de `knowledge-base/08` (módulos y variables de entorno), `knowledge-base/12` (entornos y CI) y DD-13 (Flyway con `ddl-auto: validate`).

## Goals / Non-Goals

**Goals:**
- Un proyecto que cualquiera de las 5 integrantes clona y compila con `./gradlew build` usando solo JDK 17 (el *wrapper* baja Gradle).
- Las convenciones que las fichas siguientes heredan: paquetes por módulo, formato de errores, migraciones y base de tests de integración.
- CI que corre todos los tests, incluidos los de Testcontainers, en cada PR.

**Non-Goals:**
- Entidades de dominio, tenancy, auth y endpoints (C-04 en adelante).
- `GET /api/health` y `/api/internal/tick` (C-03).
- Despliegue y elección de hosting (C-03).
- Frontend (C-02).

## Decisions

### D1 — Generar con Spring Initializr y no a mano
Se descarga un ZIP de `start.spring.io` con estos parámetros:
- `type=gradle-project` (Groovy DSL), `language=java`, `bootVersion=4.2.0`, `javaVersion=17`.
- `groupId=ar.turnos`, `artifactId=backend`, `packageName=ar.turnos`, `packaging=jar`.
- Dependencias: `web`, `security`, `data-jpa`, `validation`, `mail`, `flyway`, `postgresql` y `testcontainers`.

El ZIP se baja a una carpeta temporal fuera del repositorio. Se revisa su contenido antes de copiarlo a `backend/`. Ningún script del ZIP se ejecuta antes de esa revisión.

- **Por qué**: trae un *wrapper* de Gradle oficial y versiones de dependencias alineadas con el BOM de Spring Boot, sin necesidad de tener Gradle instalado.
- **Alternativa descartada**: escribir el build a mano. Requiere conseguir `gradle-wrapper.jar` de otra forma y es más propenso a errores de versiones.
- **Groovy DSL y no Kotlin DSL**: el equipo trabaja en Java; menos sintaxis nueva.

### D2 — Java 17 con toolchain explícito
`java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }`. El CI usa Temurin 17.

- **Por qué**: Java 17 es el JDK instalado y una versión LTS soportada por Boot 4.
- **Riesgo**: el supuesto SU-01 (Java 21) queda reemplazado. Se registra en `knowledge-base/09`.

### D3 — Paquetes por módulo con marcador
Se crea cada paquete de módulo (`shared`, `auth`, `consultorio`, `agenda`, `turnos`, `listaespera`, `pacientes`, `clinica`, `notificaciones`, `auditoria`, `reportes` y `admin`) con un `package-info.java` que documenta su responsabilidad.

- **Por qué**: Git no versiona carpetas vacías, y el `package-info.java` deja la convención explícita para que las 5 personas ubiquen su código sin dudar.
- **Alternativa descartada**: archivos `.gitkeep`, que no documentan nada.

### D4 — Errores con `ProblemDetail` nativo de Spring
- `spring.mvc.problemdetails.enabled=true`.
- Un `@RestControllerAdvice` en `shared` que extiende `ResponseEntityExceptionHandler`:
  - Agrega la propiedad `errors` (campo y mensaje) a los errores de validación.
  - Mapea toda excepción no controlada a 500, con un `detail` genérico, y registra el detalle solo en el log del servidor.
- Satisface la spec `api-errors`.
- **Alternativa descartada**: un formato propio. RFC 9457 es estándar y Spring ya lo soporta.

### D5 — Flyway con `ddl-auto: validate`
- `V1__extensiones.sql` contiene `CREATE EXTENSION IF NOT EXISTS btree_gist;`.
- `spring.jpa.hibernate.ddl-auto=validate` y `spring.flyway.validate-on-migrate=true` (valor por defecto, que detecta checksums cambiados).
- Satisface la spec `schema-migrations` y DD-13.
- **Convención de numeración**: los números de migración siguen los reservados en `CHANGES.md` (V2 en C-04, etc.), para evitar choques entre ramas en paralelo.

### D6 — Configuración por variables de entorno y perfiles
- `application.yml` usa `${SPRING_DATASOURCE_URL}` y el resto de las variables de `knowledge-base/08 §Variables de entorno`.
- Perfil `dev`: valores locales por defecto (PostgreSQL y Mailpit del `docker-compose.yml`).
- Perfil `prod`: sin valores por defecto para los secretos, así que falla si faltan.
- Ningún secreto en el repositorio. Se versiona un `.env.example` sin valores reales.

### D7 — Base de Testcontainers reutilizable
- Clase abstracta `AbstractIntegrationTest` en `src/test/java/ar/turnos/shared/` con `@SpringBootTest`, un contenedor `postgres:15` compartido y `@ServiceConnection`.
- Un test de humo verifica que el contexto arranca, que Flyway aplicó V1 y que `btree_gist` está instalada.
- **Por qué**: cada ficha posterior extiende la clase y obtiene una base real (regla dura: nada de H2).

### D8 — Seguridad inicial: todo cerrado
- `SecurityFilterChain` mínimo: niega todo lo que no se habilita explícitamente, sin login de formulario ni HTTP Basic, y con CSRF activado.
- C-03 y C-06 abren las rutas que correspondan.
- **Por qué**: evita que un endpoint nuevo quede público por omisión.

### D9 — CI en GitHub Actions
- `.github/workflows/backend.yml` corre en `pull_request` y en `push` a `main`, con filtro de rutas `backend/**` y el propio workflow.
- Pasos: `actions/setup-java` (Temurin 17) con caché de Gradle y `./gradlew build` en `backend/`.
- Los runners `ubuntu-latest` traen Docker, así que los tests de Testcontainers corren ahí.

### D10 — `.gitattributes` con `* text=auto eol=lf`
- `gradlew` queda con LF y los `*.bat`/`*.cmd` con CRLF.
- **Por qué**: el equipo mezcla sistemas operativos. Hoy git avisa conversiones LF→CRLF en cada commit, y un `gradlew` con CRLF falla en Linux y en CI.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI), 2026-11-28
- Mockups: —
- Approval: —
- Preview: —
- Visual verify: —

## Risks / Trade-offs

- **Sin Docker local, el test de integración no corre en la máquina de la responsable** → se valida en CI antes de fusionar. En local se corren `./gradlew build -x test` y los tests unitarios. Cuando instale Docker, `./gradlew build` completo.
- **Spring Boot 4 es más nuevo que lo documentado (3.x)**: hay menos ejemplos, y Jackson 3 y Hibernate 7 traen cambios → se fija la versión exacta (4.2.0). Las fichas siguientes consultan la documentación de Boot 4.
- **El ZIP de Initializr es contenido descargado** → se baja a una carpeta temporal aislada, se inspecciona la lista de archivos, y el `gradlew` se ejecuta recién después de copiarlo y revisarlo.
- **El primer `./gradlew` descarga Gradle y las dependencias desde Internet** → es el comportamiento estándar del *wrapper* y entra en la descarga aprobada.
- **Configuración que falla en `prod` si falta una variable** → es deseado: mejor fallar al arrancar que correr con valores por defecto inseguros.

## Migration Plan

No aplica: no hay sistema previo. Para revertir, basta con eliminar `backend/`, `docker-compose.yml`, el workflow y `.gitattributes`, y restaurar los documentos.
