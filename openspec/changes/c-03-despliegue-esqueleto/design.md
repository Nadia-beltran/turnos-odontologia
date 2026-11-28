# Design

## Context

- C-01 dejó el backend cerrado: `SecurityConfig` exige autenticación en toda ruta y tiene CSRF activo (con el repositorio por sesión por defecto). Todavía no hay rutas públicas.
- El perfil `prod` de `application.yml` referencia `${SPRING_MAIL_*}` sin valores por defecto, y `RequiredEnvironment.PROD` las exige. Ningún bean usa `JavaMailSender` todavía.
- `frontend/public/_redirects` ya tiene el formato de Netlify, con el placeholder `BACKEND_POR_DEFINIR`.
- `V1__extensiones.sql` crea `btree_gist`, y Flyway corre al arrancar.
- No existe `Dockerfile`. CI compila y testea, pero no despliega.
- Hosting confirmado por la responsable el 2026-11-28: Netlify, Render gratuito con Docker, Neon gratuito y cron-job.org.

## Goals / Non-Goals

**Goals:**
- Esqueleto desplegado de punta a punta que demuestre el mismo origen, la cookie de primera parte, `btree_gist` en Neon y el *keep-alive*.
- Tres rutas mínimas (salud, tick y sonda de cookie), seguras por defecto y cubiertas por tests.
- Pasos de despliegue reproducibles por otra persona del equipo.

**Non-Goals:**
- Proveedor y envío de email, lógica del tick y `@Scheduled` (C-05 y C-15).
- Despliegue automático desde CI, dominio propio (PA-08), backups adicionales (PA-13) y staging.
- Cambios al CSRF global o a la sesión (C-06).

## Decisions

### D1. Hosting (DD-16)
Netlify + Render + Neon + cron-job.org.
- **Netlify**: el `_redirects` con estado `200` hace proxy a un dominio externo y además resuelve el *fallback* SPA. Cloudflare Pages se descartó porque requiere un Worker para el proxy.
- **Render gratuito**: 512 MB, HTTPS, secretos, sin tarjeta y 750 h/mes (alcanza para un servicio 24/7). Se apaga a los 15 minutos sin tráfico, y lo mitiga el tick cada 10 minutos. Koyeb se descartó porque pide tarjeta, y Fly.io porque no tiene plan gratuito.
- **Neon gratuito**: PG 17 con `btree_gist`, sin vencimiento. La base gratuita de Render se descartó porque vence a los 30 días, y Supabase porque se pausa después de 7 días.
- **cron-job.org**: permite `POST` con headers propios.
- La DD-16 anota los límites vigentes con fecha y aclara que hay que reconfirmarlos en las páginas oficiales.

### D2. Email diferido a C-05
Render gratuito bloquea el SMTP saliente (25, 465 y 587). Se sacan las cuatro `SPRING_MAIL_*` de `RequiredEnvironment.PROD` y el bloque `spring.mail` del perfil `prod`. Si quedara, el placeholder sin resolver rompería el arranque o activaría la autoconfiguración de mail. C-05 elige un proveedor con API HTTP (con `APP_MAIL_API_KEY`) y vuelve a declarar sus variables obligatorias.
- Alternativa descartada: valores de relleno en Render, porque ocultan la falta de configuración real.

### D3. Seguridad: cadenas separadas
Se agrega un `SecurityFilterChain` con `@Order(1)` y `securityMatcher` limitado a las tres rutas exactas (`/api/health`, `/api/internal/tick` y `/api/internal/cookie-probe`). No se usa `/api/internal/**`, para que cualquier ruta interna futura nazca cerrada y tenga que abrirse a propósito. Esa cadena tiene:
- `permitAll`, sesión `STATELESS`, CSRF desactivado solo en esa cadena, sin `RequestCache` y sin `SecurityContext` persistido;
- la protección del tick en un filtro propio de esa cadena (o en el controller), no en Spring Security, para no inventar una autenticación.
La cadena existente queda igual para todo lo demás.
- Alternativa descartada: `ignoring()` en `WebSecurityCustomizer`, porque saca las rutas de todos los filtros, incluidos los headers de seguridad.
- Alternativa descartada: `csrf.ignoringRequestMatchers` en la cadena global, porque mezcla políticas y deja la sesión habilitada.

### D4. Validación del secreto del tick
`MessageDigest.isEqual` sobre los bytes UTF-8 del header y de `app.internal-tick-secret`. Header ausente → 401. Se registra solo el evento (`tick rechazado`), nunca los valores. El 401 usa `ProblemResponses` con el mismo `detail` en los dos casos (spec `api-errors`). Con el secreto correcto la respuesta es 204. Tiene su propio prefijo de paquete, `ar.turnos.shared.internal`, para que C-05 conecte ahí el procesamiento.

### D5. Salud sin base
Un controller devuelve `Map.of("status","UP")` y no se agrega Actuator. Actuator con el *health indicator* de la base consultaría Neon en cada *health check* y sumaría dependencias y superficie. Si más adelante hace falta *readiness*, se evalúa aparte.

### D6. Sonda de cookie
`GET /api/internal/cookie-probe` arma la cookie con `ResponseCookie`: `cookie_probe=1`, `Path=/api`, `HttpOnly`, `SameSite=Lax`, `Max-Age=300` y `Secure` cuando `request.isSecure()`. Para eso hace falta `server.forward-headers-strategy: framework` en `prod`, porque Render y Netlify terminan TLS y reenvían `X-Forwarded-Proto`. Lee la cookie entrante para armar `{"received": …}`. La sonda queda en producción: es inocua, sin datos ni sesión, y C-06 la puede reutilizar como prueba de regresión del mismo origen. Si se la quiere borrar, alcanza con quitar una clase.

### D7. Imagen Docker
`backend/Dockerfile` multi-etapa:
- `eclipse-temurin:17-jdk` corre `./gradlew bootJar -x test`, porque los tests corren en CI;
- `eclipse-temurin:17-jre` ejecuta con usuario no root;
- `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"` y `SPRING_PROFILES_ACTIVE=prod`;
- `EXPOSE 8080` y `server.port: ${PORT:8080}` (Render inyecta `PORT`).
Render construye desde `backend/` con el *health check path* `/api/health`. `spring.main.lazy-initialization` no se activa por ahora: si el arranque en frío se vuelve un problema, se mide y se decide en otro change.

### D8. Conexión a Neon
`SPRING_DATASOURCE_URL=jdbc:postgresql://<host>.neon.tech/<db>?sslmode=require`, apuntando al *endpoint* directo (sin *pooler*) para que Flyway use sesiones normales. HikariCP se acota con `maximum-pool-size: 3` y `minimum-idle: 0` en `prod`, para que Neon pueda apagarse sin conexiones abiertas que lo mantengan despierto.

### D9. Reenvío
En `_redirects` se reemplaza el placeholder por `https://<servicio>.onrender.com/api/:splat`. El orden ya es el correcto (`/api/*` antes del *fallback*). Netlify reenvía `Set-Cookie` sin `Domain`, así que la cookie queda en el origen de Netlify.

### D10. UI Mockups
- Decision: no (no UI), 2026-11-28. El change no agrega ni cambia pantallas.

## Risks / Trade-offs

- **Las condiciones de los planes gratuitos cambian** → la DD-16 se escribe con fecha y fuentes, y `docs/despliegue.md` explica cómo migrar cada pieza.
- **Arranque en frío de Render (alrededor de un minuto)** si el cron falla → cron cada 10 minutos (por debajo del corte de 15) y alertas de falla de cron-job.org al email de m1.
- **El cron mantiene Neon despierto si el tick toca la base** → en este change no la toca. C-05 tiene que medir las CU-hours (100/mes) cuando el tick procese el outbox.
- **512 MB con una JVM de Spring Boot** → flags de memoria y verificación del uso real en el panel de Render. Si no alcanza, se reevalúa el hosting (riesgo alto para PA-03).
- **Ruta exenta de CSRF** → solo acepta el secreto, no lee cookies ni crea sesión, y tiene tests de que la cadena general sigue exigiendo CSRF.
- **El email queda sin resolver** → PA-03 queda resuelta en parte, con un bloqueo explícito para C-05 en `10_preguntas_abiertas.md`.
