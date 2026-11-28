# Tasks

## 1. Proyecto base desde Spring Initializr

- [x] 1.1 Descargar el ZIP de Spring Initializr con los parámetros de design D1 en una carpeta temporal fuera del repositorio, listar su contenido y verificar que solo trae el proyecto Gradle esperado (sin binarios inesperados, salvo `gradle-wrapper.jar`)
- [x] 1.2 Copiar el proyecto a `backend/` y verificar que `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/` y `backend/build.gradle` existen
- [x] 1.3 Agregar `.gitattributes` en la raíz (`* text=auto eol=lf`, `*.bat`/`*.cmd` con CRLF, `*.jar` binario) y verificar con `git check-attr eol backend/gradlew` que da `lf`
- [x] 1.4 Fijar el toolchain de Java 17 en `build.gradle` y verificar que `./gradlew -v` y `./gradlew compileJava` terminan bien con JDK 17

## 2. Estructura de módulos y configuración

- [x] 2.1 Crear los 12 paquetes de módulo bajo `ar.turnos`, cada uno con un `package-info.java` que describe su responsabilidad (design D3), y verificar que `./gradlew compileJava` pasa
- [x] 2.2 Escribir `application.yml` con las variables de entorno de `knowledge-base/08`, perfiles `dev` y `prod`, `ddl-auto: validate` y `problemdetails.enabled: true` (design D5 y D6). Verificar que el perfil `prod` sin `SPRING_DATASOURCE_URL` falla al arrancar con un mensaje claro
- [x] 2.3 Agregar `.env.example` sin valores reales y verificar que `.env` está en `.gitignore`
- [x] 2.4 Crear `docker-compose.yml` con `postgres:15` y Mailpit, con puertos y credenciales de desarrollo que coincidan con el perfil `dev`. Verificar la sintaxis con la validación YAML del CI o de un editor (Docker no está instalado en local)

## 3. Migraciones (spec `schema-migrations`)

- [x] 3.1 Crear `backend/src/main/resources/db/migration/V1__extensiones.sql` con `CREATE EXTENSION IF NOT EXISTS btree_gist;`
- [x] 3.2 Crear `AbstractIntegrationTest` con Testcontainers `postgres:15` y `@ServiceConnection` (design D7), más un test que verifica que el contexto arranca, que Flyway registró V1 y que `btree_gist` figura en `pg_extension`. Verificar que compila (`./gradlew compileTestJava`); se ejecuta en CI

## 4. Errores de la API (spec `api-errors`)

- [x] 4.1 Implementar el `@RestControllerAdvice` global en `ar.turnos.shared` (design D4): `errors` por campo en la validación y 500 genérico sin detalles internos
- [x] 4.2 Escribir tests de slice web (`@WebMvcTest`, sin base de datos) con un controller de prueba solo de test. Cubren: ruta inexistente → 404 problem+json; cuerpo inválido → 400 con `errors`; excepción con mensaje técnico → 500 sin traza ni mensaje original. Verificar que `./gradlew test --tests '*Problem*'` pasa en local

## 5. Seguridad inicial

- [x] 5.1 Configurar un `SecurityFilterChain` que niega todo por defecto, sin login de formulario ni HTTP Basic, y con CSRF activo (design D8). Verificar con un test de slice que una ruta cualquiera devuelve 401 o 403 en formato Problem Details

## 6. Integración continua

- [x] 6.1 Crear `.github/workflows/backend.yml` (design D9: Temurin 17, caché de Gradle, `./gradlew build` en `backend/`, en PR y push a `main` con filtro de rutas). Verificar que el workflow corre en verde en el PR, incluyendo el test de Testcontainers de la tarea 3.2

## 7. Documentación de decisiones

- [x] 7.1 Actualizar `knowledge-base/02_descripcion_general.md` (stack: Spring Boot 4.2, Java 17, Hibernate 7), `knowledge-base/09_decisiones_y_supuestos.md` (SU-01 reemplazado por la decisión DD-15 "Java 17 + Spring Boot 4.2"), la tabla de stack de `CLAUDE.md` y el Scope de la ficha C-01 en `CHANGES.md`. Verificar con una búsqueda que no quedan menciones de "Spring Boot 3" ni "Java 21" fuera del historial de decisiones
- [x] 7.2 Agregar `backend/README.md` con cómo compilar, correr en local (perfil `dev` y docker compose) y correr los tests, incluida la nota de que el test de integración necesita Docker. Verificar que los comandos documentados funcionan tal como están escritos (salvo los que dependen de Docker)

## 8. Verificación integral

- [x] 8.1 Correr `./gradlew build -x test` y `./gradlew test` excluyendo los tests de integración en local, abrir el PR y confirmar el CI en verde. Verificar el resultado de la ficha: `./gradlew build` pasa en CI y el test de integración levanta PostgreSQL con Testcontainers

## Workflow follow-up

- PR desde `C-01-backend-scaffolding` con la plantilla del equipo. Revisa Noelia (excepción aprobada), y Nadia o Noelia por CODEOWNERS en `backend/src/main/resources/`.
- Tras el merge: marcar C-01 `[x]` en `CHANGES.md`, sincronizar las specs y archivar el change con la evidencia de uso (pendiente: telemetría no activada).
- Cuando Docker esté instalado en local, correr `./gradlew build` completo una vez y registrarlo.
