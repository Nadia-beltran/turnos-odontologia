# Despliegue y Entornos

Restricción: **presupuesto cero**. Todo corre en planes gratuitos (ver la elección pendiente en PA-03).

## Entornos

| Entorno | Frontend | Backend | Base de datos | Email |
|---|---|---|---|---|
| Local | `vite dev`, con proxy `/api` → `localhost:8080` (configurado en `vite.config.ts`) | `./gradlew bootRun` con perfil `dev` | PostgreSQL en Docker (`docker compose`) | Servidor SMTP de prueba local (por ejemplo, Mailpit en Docker): ningún email sale a Internet |
| Producción | Hosting estático gratuito con reenvío de `/api` | Servicio gratuito para contenedores o JVM | PostgreSQL gratuito administrado | Proveedor gratuito (ver 11) |

Un entorno de *staging* no entra en v1 por costo y cuotas. Las ramas se validan en local y en CI.

## Requisitos del hosting (criterios de elección para PA-03)

| Componente | Requisito obligatorio | Por qué |
|---|---|---|
| Frontend | Reenvío (*proxy rewrite*) de `/api/*` a un dominio externo, y *fallback* de rutas a `index.html` | DD-03 y router con History API |
| Backend | Al menos 512 MB de RAM, HTTPS y variables de entorno secretas | JVM de Spring Boot |
| Backend | Que no se apague, o que se pueda mantener despierto con un *ping* externo | Recordatorios, outbox y vencimiento de ofertas dependen de la tarea programada |
| PostgreSQL | Versión 15 o superior **con la extensión `btree_gist`** | Restricción de exclusión (DD-04) |
| PostgreSQL | Plan gratuito sin vencimiento a los 30 días ni pausa prolongada por inactividad | Datos de salud persistentes |

> Las ofertas y condiciones de los planes gratuitos cambian con frecuencia. La elección final se documenta como una DD nueva, con los límites vigentes el día que se elija.

## El problema de la app que "duerme"

Muchos planes gratuitos apagan el servicio tras unos minutos sin tráfico. Consecuencias:

- **Las tareas programadas no corren**: no salen recordatorios ni ofertas, y las ofertas no vencen.
- **Arranque en frío** de la JVM (decenas de segundos): la primera reserva del paciente es lenta.

**Mitigación propuesta**:

1. Endpoint interno `POST /api/internal/tick`, protegido con el header `X-Tick-Secret` (`APP_INTERNAL_TICK_SECRET`). Procesa el outbox, los vencimientos y los recordatorios igual que la tarea programada, de forma idempotente.
2. Un *cron* externo gratuito llama a ese endpoint cada 5 a 10 minutos. Así mantiene despierto el servicio y garantiza el procesamiento aunque el `@Scheduled` interno no corra.
3. Revisar que las llamadas periódicas no agoten las horas mensuales gratuitas del hosting.
4. Ajustar la JVM para memoria baja (`-Xmx` acotado, `spring.main.lazy-initialization` evaluado, sin módulos innecesarios).

## Integración continua (CI)

- **Backend**: `./gradlew build` (compila, corre los tests unitarios y de integración con Testcontainers).
- **Frontend**: `npm ci && npm run build` (verificación de tipos con `tsc --noEmit` y compilación).
- Cada PR corre el pipeline completo. La rama `main` solo recibe merges con CI en verde (se refuerza en la fase team-mode).
- Las migraciones de Flyway se aplican al arrancar el backend en producción. Las migraciones destructivas requieren revisión explícita en el PR.

## Copias de seguridad y datos

- Primero se usan las copias del plan gratuito de la base, verificando su existencia y retención.
- Si no alcanzan: `pg_dump` periódico, **cifrado** antes de salir del entorno. Nunca se guardan dumps sin cifrar con datos de pacientes en artefactos de CI ni en el repositorio (PA-13).
- Las bases de desarrollo usan solo datos ficticios (seed de desarrollo).
