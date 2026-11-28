# Tasks

> Governance ALTO: el código pasa por revisión antes de desplegarse. Las tareas marcadas **[m1]** requieren cuentas o paneles de proveedores y las hace la responsable; el agente prepara los pasos y verifica el resultado. Ningún secreto real se escribe en el repo, en logs ni en el handoff.

## 1. Configuración del perfil prod (spec `runtime-configuration`, design D2, D6 y D8)

- [x] 1.1 Escribir primero, en `RequiredEnvironmentTest`, los casos de la spec: configuración completa sin `SPRING_MAIL_*` no falla, falta `APP_INTERNAL_TICK_SECRET`, faltan dos variables a la vez (las nombra a ambas y no muestra valores). Verificar que fallan
- [x] 1.2 Quitar las cuatro `SPRING_MAIL_*` de `RequiredEnvironment.PROD` con un comentario que remita a C-05. Verificar con `./gradlew test --tests '*RequiredEnvironmentTest'`
- [x] 1.3 En el perfil `prod` de `application.yml`: quitar el bloque `spring.mail`, agregar `server.port: ${PORT:8080}`, `server.forward-headers-strategy: framework` y HikariCP `maximum-pool-size: 3` y `minimum-idle: 0`. Verificar que el perfil `dev` no cambia (`git diff` acotado al bloque `prod`)

## 2. Endpoints mínimos (specs `service-health` e `internal-tick`, design D3 a D6)

- [x] 2.1 Escribir los tests web (`@WebMvcTest` o `MockMvc` con la configuración de seguridad real) para cada escenario:
  - `GET /api/health`: 200 y `{"status":"UP"}` sin cookies, y sin `Set-Cookie` de sesión.
  - Tick: secreto correcto → 204 sin `Set-Cookie`; secreto incorrecto o sin header → 401 Problem Details con el mismo `detail`; sin token CSRF igual responde 204.
  - Sonda: primera llamada → `{"received":false}` y `Set-Cookie` con `HttpOnly`, `SameSite=Lax`, `Path=/api` y `Max-Age=300`; con la cookie → `{"received":true}`; por HTTPS (`secure(true)`) → `Secure`. En producción, `X-Forwarded-Proto` se traduce con `forward-headers-strategy: framework` (1.3) y se verifica en 5.3.
  - Regresión: un `POST` a otra ruta sin sesión sigue rechazado por CSRF (403), y una ruta `/api/internal/` no declarada responde 401 (la cadena general no cambió).

  Verificar que fallan
- [x] 2.2 Crear `shared/web/HealthController` (sin acceso a la base). Verificar con un test que `/api/health` responde sin `DataSource` disponible, o que no inyecta repositorios
- [x] 2.3 Crear `shared/internal/TickController` con la comparación en tiempo constante (`MessageDigest.isEqual`) contra `app.internal-tick-secret` y un log sin valores. Crear `shared/internal/CookieProbeController` con `ResponseCookie`
- [x] 2.4 Agregar en `SecurityConfig` la cadena `@Order(1)` con `securityMatcher` limitado a las tres rutas exactas, `permitAll`, `STATELESS` y CSRF desactivado solo ahí (design D3). Verificar que pasan todos los tests de 2.1 y que siguen verdes `ApiProblemDetailsTest` y las suites de tenancy y auditoría (`./gradlew build`; los de integración, en CI si no hay Docker local)
- [x] 2.5 Verificar con una búsqueda (`grep`) que ningún log ni mensaje de excepción de `shared/internal` interpola el header ni la propiedad del secreto

## 3. Imagen del backend (design D7)

- [x] 3.1 Crear `backend/Dockerfile` multi-etapa (JDK 17 para `bootJar -x test`, JRE 17 sin root, `JAVA_TOOL_OPTIONS` de poca memoria, `SPRING_PROFILES_ACTIVE=prod`) y `backend/.dockerignore` (`build/`, `.gradle/`, `.env*`)
- [ ] 3.2 Verificar en local, si hay Docker: `docker build -t turnos-backend backend/`; correr el contenedor con `PORT=8080`, variables ficticias y el PostgreSQL de `docker-compose.yml`; `curl /api/health` → 200; `docker stats` con un límite de 512 MB sin OOM. Sin Docker local, registrar la verificación como pendiente para el primer despliegue en Render

## 4. Infraestructura (design D1, D8 y D9)

- [ ] 4.1 **[m1]** Crear el proyecto en Neon (PG 17, región más cercana a la de Render) y anotar el host del *endpoint* directo. El agente verifica después del primer arranque: `SELECT extname FROM pg_extension WHERE extname='btree_gist'` devuelve una fila y `flyway_schema_history` tiene V1 a V3
- [ ] 4.2 **[m1]** Crear el Web Service en Render (Docker, raíz `backend/`, plan Free, *health check* `/api/health`, autodeploy desde `main`) y cargar las variables obligatorias de `prod`. El agente arma la lista sin valores y genera el comando para crear un `APP_INTERNAL_TICK_SECRET` aleatorio de 32 bytes
- [ ] 4.3 Reemplazar `BACKEND_POR_DEFINIR` en `frontend/public/_redirects` por el host real de Render
- [ ] 4.4 **[m1]** Crear el sitio en Netlify (base `frontend/`, build `npm ci && npm run build`, publicación `dist`), confirmar que `_redirects` llega a `dist/` y anotar la URL pública. Con esa URL, actualizar `APP_BASE_URL` en Render
- [ ] 4.5 **[m1]** Crear el cron en cron-job.org: `POST https://<frontend>/api/internal/tick` cada 10 minutos, header `X-Tick-Secret`, notificación de fallas al email de m1. El agente verifica en el historial del cron o en los logs de Render al menos tres respuestas 204 seguidas

## 5. Verificación en producción (Resultado de la ficha)

- [ ] 5.1 `curl -i https://<frontend>/api/health` → 200 `{"status":"UP"}`; `curl -i https://<frontend>/panel` → `index.html`
- [ ] 5.2 Tick sin secreto → 401 Problem Details; con un secreto incorrecto → 401
- [ ] 5.3 **[m1]** Prueba manual de cookie en el navegador: abrir `https://<frontend>/api/internal/cookie-probe` dos veces; la segunda devuelve `{"received":true}` y en DevTools la cookie figura con el dominio de Netlify, `SameSite=Lax`, `Secure` y `HttpOnly`. Guardar una captura sin datos sensibles
- [ ] 5.4 Revisar en el panel de Render la memoria en reposo y después de las pruebas, y anotar el valor en `docs/despliegue.md`

## 6. Documentación y decisiones

- [x] 6.1 Agregar la **DD-16** en `knowledge-base/09_decisiones_y_supuestos.md`: elección, alternativas descartadas, límites vigentes al 2026-11-28 con fuentes y trade-offs
- [x] 6.2 En `knowledge-base/10_preguntas_abiertas.md`, dejar PA-03 resuelta en parte (hosting por DD-16) y mover el proveedor de email a una pregunta explícita para C-05: API HTTP obligatoria porque Render bloquea el SMTP
- [x] 6.3 Actualizar `knowledge-base/12_despliegue_y_entornos.md` (tabla de producción) y `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno (las `SPRING_MAIL_*` pasan a opcionales hasta C-05 y se suma `PORT`), y las filas Email y Hosting de `CLAUDE.md`
- [x] 6.4 Escribir `docs/despliegue.md`: arquitectura (diagrama Mermaid navegador → Netlify → Render → Neon, más cron-job.org → tick), alta de cada servicio, variables sin valores, generación y rotación del secreto, verificación (sección 5), cómo diagnosticar un arranque fallido y cómo migrar cada pieza si cambia un plan gratuito
- [x] 6.5 Crear y mantener `knowledge-base/entrega/C-03.md` (handoff de equipo) con la evidencia de las secciones 3 a 5
- [ ] 6.6 Correr `openspec validate c-03-despliegue-esqueleto` y `./gradlew build` y `npm run build` en verde antes de pedir autorización para el PR
