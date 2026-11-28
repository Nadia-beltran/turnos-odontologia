# Proposal

## Why

PA-03, de prioridad alta, sigue abierta: todavía no sabemos si los planes gratuitos aguantan lo que la arquitectura da por hecho. Eso incluye el reenvío de `/api` en el mismo origen (DD-03), `btree_gist` (DD-04), una JVM con 512 MB y un *keep-alive* para que la tarea programada corra. Desplegar ahora un esqueleto en producción valida esos supuestos antes de que el dominio dependa de ellos (C-05 y siguientes).

## What Changes

- Decisión de hosting registrada como **DD-16**: Netlify para el frontend, Render (plan gratuito, Docker) para el backend, Neon (plan gratuito) para PostgreSQL y cron-job.org como cron externo. Con esto **PA-03 queda resuelta en parte**. El proveedor de email se difiere a C-05 porque Render gratuito bloquea el SMTP saliente (puertos 25, 465 y 587); C-05 tiene que elegir un proveedor con API HTTP.
- Nuevo endpoint público `GET /api/health`, que responde el estado del servicio sin datos sensibles. Lo usan el *health check* de Render y la verificación del reenvío.
- Nuevo endpoint `POST /api/internal/tick`, protegido con el header `X-Tick-Secret` contra `APP_INTERNAL_TICK_SECRET`. En este change no tiene lógica: responde y mantiene despierto el servicio. Queda exento de CSRF porque no usa cookies de sesión.
- Nueva sonda `GET /api/internal/cookie-probe`, que emite una cookie de prueba `SameSite=Lax` para comprobar a mano que las cookies del backend llegan como cookies de primera parte a través del reenvío.
- Perfil `prod`: **BREAKING** (solo de configuración). `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD` dejan de ser obligatorias hasta C-05. El resto de las variables obligatorias siguen igual.
- `backend/Dockerfile` multi-etapa con la JVM ajustada para poca memoria.
- En `frontend/public/_redirects`, la URL real del backend en Render reemplaza el placeholder `BACKEND_POR_DEFINIR`.
- `docs/despliegue.md` con los pasos reproducibles: cuentas, variables, cron y la verificación manual de la cookie.
- Actualización de `knowledge-base/09`, `10` y `12` y de `CLAUDE.md` con el hosting elegido.

## Capabilities

### New Capabilities
- `service-health`: disponibilidad observable del backend en producción. Cubre el endpoint público de salud y la sonda de cookie de primera parte a través del reenvío.
- `internal-tick`: punto de entrada protegido por secreto que llama el cron externo para mantener el servicio despierto. En los changes siguientes va a disparar el procesamiento periódico (outbox, vencimientos, recordatorios).
- `runtime-configuration`: variables de entorno obligatorias por perfil y falla temprana del arranque cuando falta alguna.

### Modified Capabilities
- (ninguna) El reenvío de `/api` y el *fallback* ya los cubre `frontend-routing`, y `btree_gist` ya lo cubre `schema-migrations`. Este change los verifica en producción sin cambiar sus requisitos.

## Impact

- **Código**: `backend/src/main/java/ar/turnos/shared/` (nuevos `web/HealthController`, `internal/TickController` y `CookieProbeController`, `security/SecurityConfig` y `config/RequiredEnvironment`), `backend/Dockerfile` y `frontend/public/_redirects`.
- **APIs**: tres rutas nuevas bajo `/api`. Son las primeras rutas públicas del backend.
- **Seguridad** (governance ALTO): dos rutas sin sesión y una ruta exenta de CSRF protegida por secreto. El secreto se compara en tiempo constante y nunca se registra en logs.
- **Infraestructura**: cuentas en Netlify, Render, Neon y cron-job.org, que crea la responsable (m1). Variables de entorno de producción en Render.
- **Fuera de alcance**: el proveedor de email y el envío (C-05), la lógica del tick (C-05 y C-15), el dominio propio (PA-08), las copias de seguridad adicionales (PA-13) y el despliegue automático desde CI.
