# Proposal

## Why

El email es el único canal con el paciente en v1 (DD-06) y de él depende el diferencial del producto: confirmación, recordatorio, gestión y ofertas de la lista de espera. DD-09 y RN-NO-01 exigen que ningún email se pierda si el proveedor falla o si la transacción del evento se revierte, y la cuota diaria del plan gratuito (KB 11) obliga a priorizar. C-05 es el primer paso de la Fase 2 del camino crítico: C-06 (verificación de email y recuperación de contraseña) y todas las fichas de turnos y lista de espera encolan emails a través de este mecanismo.

## What Changes

- Migración `V4__notificacion.sql`: tabla `notificacion` (outbox) con `consultorio_id` nullable, `tipo`, `prioridad`, `destinatario`, `payload` jsonb, `estado`, `intentos`, `programada_para`, `enviada_at` y `ultimo_error`, con el índice (`estado`, `programada_para`).
- `NotificacionService.encolar(tipo, destinatario, payload, programadaPara)`: inserta la notificación **dentro de la transacción de quien llama** (patrón outbox). Toma el consultorio del contexto de tenancy, nunca de un parámetro.
- Procesador del outbox: selecciona con `FOR UPDATE SKIP LOCKED` las notificaciones `PENDIENTE` vencidas, ordenadas por prioridad (1 a 4, derivada del tipo) y por `programada_para`, y las envía respetando `APP_MAIL_DAILY_QUOTA`:
  - Por debajo del 80 % de la cuota envía todas las prioridades.
  - Desde el 80 % envía solo las prioridades 1 y 2.
  - Al 100 % deja todo en `PENDIENTE` hasta el día siguiente.
- Reintentos: un envío fallido se reprograma a los 5, 15 y 60 minutos; si falla el cuarto intento queda `FALLIDA`, con el último error guardado.
- Un consultorio `SUSPENDIDO` no envía emails (RN-MT-03): sus notificaciones quedan en `PENDIENTE`.
- Plantillas Thymeleaf (núcleo, sin integración MVC) en `templates/email/`, en versión HTML y texto plano, con remitente visible "{consultorio} vía {BRAND_NAME}" (DD-14) y `Reply-To` al email del consultorio. C-05 entrega el mecanismo y el layout común; el texto de cada tipo lo escribe la ficha que lo encola.
- **Ejecución periódica compartida**: una única entrada (`TickRunner`) que corre todas las tareas periódicas registradas. La invocan la tarea `@Scheduled` cada 5 minutos y el endpoint `POST /api/internal/tick`, así el tick procesa exactamente lo mismo que la tarea programada. El procesador del outbox es la primera tarea registrada.
- **El endpoint `POST /api/internal/tick` y su header `X-Tick-Secret` son de C-03**: C-05 no los crea. Quien fusione segundo agrega la llamada de una línea del controller a `TickRunner` (design D2).
- Envío detrás de un puerto propio (`EnviadorEmail`) con una implementación SMTP sobre Spring Mail: funciona con Mailpit en desarrollo y en tests, y no ata la elección del proveedor (PA-03).

## Capabilities

### New Capabilities
- `email-notifications`: encolado transaccional de emails, aislamiento por consultorio, procesamiento por prioridad con cuota diaria, reintentos con espera creciente, plantillas HTML y texto plano y remitente por consultorio.
- `periodic-tasks`: ejecución de las tareas periódicas del backend desde un único punto, que comparten la tarea programada y el tick externo, de forma idempotente.

### Modified Capabilities
<!-- Ninguna: tenant-isolation, schema-migrations y api-errors no cambian sus requisitos. V4 se rige por la spec schema-migrations vigente y notificacion aplica el filtro de tenant-isolation tal como está. -->

## Impact

- **Código nuevo** (zona de C-05):
  - `backend/src/main/java/ar/turnos/notificaciones/`
  - `backend/src/main/resources/templates/email/` (layout común)
  - `backend/src/main/resources/db/migration/V4__notificacion.sql`
  - Tests en `backend/src/test/java/ar/turnos/notificaciones/` y plantillas solo de test en `backend/src/test/resources/templates/email/`.
- **Fuera de la zona de la ficha, acotado**:
  - `backend/src/main/java/ar/turnos/shared/tick/` (`TareaPeriodica`, `TickRunner`): lo comparten C-03, C-15 y C-17. Ver design D2.
  - `TestcontainersConfiguration` (tests, `shared`): suma un contenedor de Mailpit.
  - `application.yml`: claves `app.brand-name`, `app.mail.lote` y `app.tick.programado`.
- **No se toca**: `SecurityConfig`, `RequiredEnvironment` (las variables de mail ya están), migraciones V1 a V3 ni los paquetes de otras fichas.
- **Dependencias nuevas**: `org.thymeleaf:thymeleaf` (núcleo, versión del BOM de Boot 4.2.0; sin el starter, para no sumar resolución de vistas MVC). Para tests, una imagen de Mailpit con versión fija vía `GenericContainer` de Testcontainers.
- **Desbloquea**: C-06 (verificación y recuperación) y, a través de él, el resto del dominio.
