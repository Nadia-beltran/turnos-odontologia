# Design

## Context

La motivación está en `proposal.md` y el comportamiento exigido, en `specs/email-notifications` y `specs/periodic-tasks`. El estado real del backend (C-01 y C-04 fusionados) condiciona el enfoque:

- Spring Boot 4.2.0, Java 17, Hibernate 7.4, `ddl-auto: validate`. Ya están `spring-boot-starter-mail` y `spring-boot-starter-mail-test`. No hay motor de plantillas.
- `application.yml` ya define `spring.mail.*`, `app.base-url`, `app.mail.from` y `app.mail.daily-quota` en `dev` y `prod`, y `RequiredEnvironment.PROD` ya exige las variables de mail y `APP_INTERNAL_TICK_SECRET`.
- Tenancy (C-04): filtro `tenant` auto-habilitado, `TenantContext.ejecutarCon` / `comoSistema`, guardas de escritura en `BaseTenantEntity`. El SQL nativo **no** pasa por el filtro. C-04 D7 ya fijó que C-05 lee `nombre`, `email_contacto` y `estado` del consultorio con un JOIN en su consulta nativa, sin crear una entidad `Consultorio` (es de C-06).
- `docker-compose.yml` levanta Mailpit (SMTP 1025, web 8025) para `dev`. Los tests usan `AbstractIntegrationTest` + `TestcontainersConfiguration` (`postgres:15`, contexto compartido).
- `SecurityConfig` cierra todo; no hay endpoints de C-05.
- C-03 (dueño de `POST /api/internal/tick` y `X-Tick-Secret`) **no** está fusionado y puede llegar antes o después que C-05.

## Goals / Non-Goals

**Goals:**
- Un outbox *at-least-once*: ninguna notificación se pierde; en el peor caso se duplica un email (ver Riesgos).
- Una sola entrada para el trabajo periódico, que el tick y la tarea programada comparten sin divergir.
- Que cada ficha posterior solo tenga que agregar su tipo de plantilla y llamar a `encolar`.

**Non-Goals:**
- El endpoint HTTP del tick y la validación de `X-Tick-Secret` (C-03, ver D2).
- El texto de cada tipo de email (lo escribe la ficha que lo encola: C-06 verificación y recuperación, C-12/C-14/C-15 turnos y recordatorio, C-17 lista de espera).
- La re-verificación previa al envío ("no recordar un turno cancelado, no ofertar un hueco tomado", KB 11): la agregan C-15 y C-17, que conocen esos estados.
- Las columnas `turno_id` y `oferta_envio_id` de KB 04: sus tablas no existen todavía. Las agregan C-10 (`V7`) y C-17 (`V8`) con un `ALTER TABLE` en su migración.
- Mostrar las `FALLIDA` en el panel, procesar rebotes, el consumo de cuota en el panel del `ADMIN_SAAS` (C-23) y el límite de 20 inscriptos por oferta (C-17).
- Elegir proveedor (PA-03, C-03).

## Decisions

### D1 — `notificacion`: tenant con `consultorio_id` nullable
- Tabla según KB 04 más una columna `prioridad smallint` (ver D4). `consultorio_id` es **nullable**: la recuperación de contraseña del `ADMIN_SAAS` y los avisos del sistema no tienen consultorio (KB 11 lista "Recuperación de contraseña" para cualquier usuario).
- Como `consultorio_id` es nullable, la entidad `Notificacion` **no** extiende `BaseTenantEntity` (que lo exige no nulo): declara `@Filter(name = "tenant")` directamente, igual que `Usuario` (C-04 D7 y `package-info` de `shared.tenancy`). Una fila sin consultorio nunca aparece bajo un consultorio.
- Guardas propias, equivalentes a las de la base: `@PrePersist` toma `TenantContext.requerido()`; en modo sistema deja el consultorio en `null` salvo que la entidad ya lo traiga. Fuera del modo sistema, sin contexto, falla cerrado (`TenantContextAusenteException`).
- `encolar` **no** recibe el consultorio (RN-MT-02): lo toma del contexto.
- **Alternativa descartada**: `consultorio_id NOT NULL` y extender `BaseTenantEntity`. Obliga a inventar un consultorio para los emails del administrador SaaS.

Esquema de `V4__notificacion.sql` (orientativo):

```sql
CREATE TABLE notificacion (
    id              uuid PRIMARY KEY,
    consultorio_id  uuid REFERENCES consultorio (id),
    tipo            varchar(40) NOT NULL CHECK (tipo IN ('VERIFICACION_EMAIL','RECUPERACION_CONTRASENA',
                        'OFERTA_HUECO','RESERVA_CONFIRMADA','TURNO_CANCELADO','TURNO_REPROGRAMADO',
                        'RECORDATORIO','INSCRIPCION_ESPERA','OFERTA_TOMADA')),
    prioridad       smallint NOT NULL CHECK (prioridad BETWEEN 1 AND 4),
    destinatario    varchar(254) NOT NULL,
    payload         jsonb NOT NULL,
    estado          varchar(20) NOT NULL CHECK (estado IN ('PENDIENTE','ENVIADA','FALLIDA')),
    intentos        smallint NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    programada_para timestamptz NOT NULL,
    enviada_at      timestamptz,
    ultimo_error    text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT notificacion_enviada CHECK ((estado = 'ENVIADA') = (enviada_at IS NOT NULL))
);
CREATE INDEX notificacion_estado_programada_idx ON notificacion (estado, programada_para);
CREATE INDEX notificacion_consultorio_idx ON notificacion (consultorio_id);
```

- Enumerados como `varchar` + CHECK, igual que `auditoria` (V3). Un tipo nuevo se suma con una migración que reemplaza el CHECK.
- `notificacion` **no** es append-only: el procesador actualiza `estado`, `intentos` y fechas.

### D2 — Tick: C-05 entrega el contrato, C-03 el endpoint
La ficha C-05 menciona `POST /api/internal/tick`, pero la ficha C-03 lo crea con su secreto. Para no tener dos dueños:

- **C-05 crea** `ar.turnos.shared.tick`:
  - `TareaPeriodica` (interfaz `void ejecutar()` con un nombre para el log).
  - `TickRunner.ejecutar()`: corre todas las `TareaPeriodica` registradas (orden por `@Order`), cada una en su `try/catch`; un fallo se loguea con el nombre de la tarea y la clase de la excepción, sin el mensaje (puede traer datos personales), y sigue con la siguiente.
  - `TickProgramado`: `@EnableScheduling` + `@Scheduled(fixedDelayString = "${app.tick.intervalo:PT5M}")` que llama a `TickRunner.ejecutar()`. Se registra solo si `app.tick.programado` es `true` (por defecto `true`).
  - `ProcesadorNotificaciones` implementa `TareaPeriodica`: es la primera tarea.
- **C-03 crea** el controller con `X-Tick-Secret` y la apertura en `SecurityConfig`. Su cuerpo es una línea: `tickRunner.ejecutar()`.
- **Quien fusione segundo hace el cableado**: si C-03 ya está en `main` cuando se aplica C-05, C-05 agrega esa línea y un test del endpoint (tarea 6.2). Si no, C-05 lo deja escrito en su handoff y C-03 lo hace. En ningún caso C-05 crea el endpoint ni toca `SecurityConfig`.
- C-15 (recordatorios) y C-17 (vencimiento de ofertas) solo agregan otra `TareaPeriodica`: el tick y la tarea programada no pueden divergir.
- **Alternativa descartada**: que C-05 cree el endpoint con su secreto si C-03 no llegó. Duplica una zona de seguridad (ALTO) y garantiza un conflicto de merge.
- **Alternativa descartada**: sin interfaz, que el controller y el `@Scheduled` llamen directo al procesador. Cada ficha futura tendría que tocar los dos lugares; es justo la divergencia que el tick debe evitar.

### D3 — Selección y envío en una transacción, serializada con un *advisory lock*
`ProcesadorNotificaciones.ejecutar()` corre en `TenantContext.comoSistema(...)` (uso permitido: proceso que recorre consultorios) y en **una** transacción:

1. `SELECT pg_try_advisory_xact_lock(<constante>)`: si otra corrida está en curso, termina sin hacer nada. Esto evita que dos corridas (tick y tarea programada, o dos instancias) superen juntas la cuota, algo que `SKIP LOCKED` solo no impide.
2. Cuenta los enviados hoy (D5) y calcula el límite y la prioridad máxima admitida (D4).
3. Selección nativa:
   ```sql
   SELECT n.id, c.nombre, c.email_contacto
   FROM notificacion n LEFT JOIN consultorio c ON c.id = n.consultorio_id
   WHERE n.estado = 'PENDIENTE' AND n.programada_para <= :ahora
     AND n.prioridad <= :prioridadMaxima
     AND (c.id IS NULL OR c.estado <> 'SUSPENDIDO')
   ORDER BY n.prioridad, n.programada_para
   LIMIT :limite
   FOR UPDATE OF n SKIP LOCKED
   ```
   `FOR UPDATE OF n` es obligatorio: PostgreSQL no permite bloquear el lado nulo de un LEFT JOIN.
4. Carga las entidades por id, renderiza (D6), envía por `EnviadorEmail` (D7) y actualiza cada una (`ENVIADA` o reintento, D5). Commit al final.

- **Excepción explícita a la regla de consultas nativas** (`package-info` de `shared.tenancy`): esta consulta no filtra por `consultorio_id` porque recorre todos los consultorios en modo sistema. Se documenta en el Javadoc del procesador y se revisa en el PR.
- El envío SMTP ocurre con las filas bloqueadas. Se acota con `app.mail.lote` (por defecto 20) y con timeouts SMTP de 10 s (`spring.mail.properties.mail.smtp.connectiontimeout`, `timeout`, `writetimeout`) en `application.yml`: el peor caso (20 × 10 s) queda por debajo del intervalo de 5 min.
- **Alternativa descartada**: reclamar filas con un estado intermedio `EN_PROCESO` y enviar fuera de la transacción. Agrega un estado que KB 04 no tiene y un barrido de reclamos huérfanos; con el volumen de v1 no compensa.

### D4 — Prioridad derivada del tipo, guardada en una columna
- `TipoNotificacion` (enum) lleva la prioridad y el asunto. Prioridades de KB 11: 1 = `VERIFICACION_EMAIL`, `RECUPERACION_CONTRASENA`, `OFERTA_HUECO`; 2 = `RESERVA_CONFIRMADA`, `TURNO_CANCELADO`, `TURNO_REPROGRAMADO`; 3 = `RECORDATORIO`; 4 = `INSCRIPCION_ESPERA` y `OFERTA_TOMADA` (ver Open Questions).
- `encolar` copia la prioridad del tipo a la columna `prioridad`, así el `ORDER BY` y el filtro de cuota son SQL simple. Cambiar la prioridad de un tipo afecta solo a lo que se encole después.
- Cuota (`Q` = `app.mail.daily-quota`, `E` = enviados hoy, `umbral = Q * 8 / 10` entero):
  - `E >= Q` → no se selecciona nada (todo queda `PENDIENTE`, sin sumar intentos) y se loguea una advertencia por día (ver Open Questions).
  - `E >= umbral` → `prioridadMaxima = 2`; si no, `4`.
  - `limite = min(lote, Q - E)`.
  - Dentro del lote se lleva la cuenta en memoria: si un envío hace cruzar el umbral, las notificaciones de prioridad 3 y 4 que siguen se dejan sin tocar.
- **Alternativa descartada**: `ORDER BY CASE tipo …` sin columna. Esconde la tabla de prioridades en SQL y no permite indexar ni consultar por prioridad.

### D5 — Reintentos y día de cuota
- Fallo de envío (cualquier excepción del puerto o del renderizado): `intentos += 1`, `ultimo_error` = clase + mensaje truncado a 500 caracteres (solo en la base, nunca en el log). Si `intentos < 4`, `programada_para = ahora + [5, 15, 60][intentos - 1]` minutos; si `intentos = 4`, `estado = FALLIDA`. Es 1 intento inicial + 3 reintentos (RN-NO-03, KB 11; ver Open Questions por Flujo 6).
- Las notificaciones salteadas por cuota o por consultorio suspendido no suman intentos.
- "Enviados hoy" = `count(*)` de `ENVIADA` con `enviada_at >= 00:00 UTC` del día actual. Índice suficiente: `(estado, programada_para)` reduce a `ENVIADA`; con el volumen de v1 el conteo es barato.
- El tiempo sale de un `Clock` (bean UTC), para poder fijarlo en tests si hace falta.

### D6 — Plantillas: Thymeleaf núcleo, motor propio
- Dependencia `org.thymeleaf:thymeleaf` (versión del BOM). **Sin** `spring-boot-starter-thymeleaf`: no hace falta la resolución de vistas de MVC y así no cambia el comportamiento de los controllers.
- `PlantillasEmail` arma un `TemplateEngine` con dos `ClassLoaderTemplateResolver` sobre `templates/email/`: `.html` en modo `HTML` (escapa con `th:text`) y `.txt` en modo `TEXT`, ambos con `checkExistence`. Cada tipo tiene `{tipo en minúsculas}.html` y `.txt`. Un `_layout.html` común aporta encabezado y pie ("{consultorio} vía Estudio Sonrisa · herramienta de turnos", mitigación de DD-14), sin imágenes remotas.
- Variables: el `payload` más `consultorio` (nombre), `marca` (`app.brand-name`) y `baseUrl` (`app.base-url`) para armar links al frontend.
- `encolar` verifica que existan las dos plantillas del tipo (chequeo de classpath) y falla si no: el error aparece en el test de la ficha que encola, no en producción como un `FALLIDA`.
- C-05 no trae plantillas de tipos reales: los tests usan plantillas solo de test en `src/test/resources/templates/email/` para tipos de prioridad 1, 2, 3 y 4.
- **Alternativa descartada**: reemplazo de `{{variable}}` a mano. Obliga a escribir y probar el escape HTML.

### D7 — Puerto `EnviadorEmail` con implementación SMTP
- `EnviadorEmail.enviar(EmailSaliente)` (destinatario, remitente visible, `Reply-To` opcional, asunto, HTML, texto). `EnviadorSmtp` usa `JavaMailSender` + `MimeMessageHelper` multiparte (`setText(texto, html)`).
- Remitente: la dirección de `app.mail.from` con el nombre visible "{consultorio} vía {app.brand-name}", o solo `app.brand-name` sin consultorio; `Reply-To` = `email_contacto` del consultorio (DD-14, KB 11).
- Si PA-03 elige un proveedor por API, se agrega otra implementación del puerto sin tocar el procesador.
- `app.brand-name` (por defecto `Estudio Sonrisa`) es la constante de configuración de DD-14; no es una variable de entorno obligatoria.

### D8 — Tests con Mailpit real
- `TestcontainersConfiguration` suma un `GenericContainer` de Mailpit con **versión fija** (no `latest`; se elige al implementar) y registra `spring.mail.host`/`port` con un `DynamicPropertyRegistrar`. Queda en el contexto compartido para no crear otro.
- Los tests consultan la API de Mailpit (`GET /api/v1/messages`, `GET /api/v1/message/{id}`) y la vacían (`DELETE /api/v1/messages`) antes de cada caso.
- `AbstractIntegrationTest` suma `app.tick.programado=false`, para que la tarea programada no compita con los tests.
- Fallos del proveedor: `@MockitoSpyBean EnviadorEmail` con `doThrow` en la clase de tests del procesador. Se simula el proveedor, nunca la base (regla dura). Esa clase fija `app.mail.daily-quota=10`: un solo contexto extra para todos los escenarios de cuota, prioridad y reintento.
- Datos de cuota ("8 enviados hoy") y de consultorios suspendidos se insertan por JDBC con datos ficticios (`example.test`).

## Risks / Trade-offs

- [Envío exitoso y commit fallido → email duplicado al reintentar] → aceptado: *at-least-once* es el contrato del outbox. Los emails son idempotentes para el paciente (el link es el mismo).
- [El SMTP lento retiene los bloqueos] → lote acotado y timeouts de 10 s (D3). Con el advisory lock, una corrida lenta hace que la siguiente simplemente no haga nada.
- [La cuota del proveedor se reinicia en otro horario que las 00:00 UTC] → configurable más adelante si PA-03 lo exige; hoy UTC.
- [Consultorio suspendido: sus `PENDIENTE` se envían tarde si se reactiva] → la re-verificación previa de C-15/C-17 descarta recordatorios y ofertas vencidos. Ver Open Questions.
- [Nueva dependencia de plantillas] → Thymeleaf núcleo, sin integración web; versión gestionada por el BOM.
- [`shared.tick` queda fuera de la zona de la ficha] → es un paquete nuevo, sin archivos existentes, y lo usan C-03, C-15 y C-17. Se avisa en el PR.
- [El contenedor de Mailpit se suma a todos los tests de integración] → ~1 s de arranque, a cambio de un solo contexto de Spring.

## Migration Plan

- `V4__notificacion.sql` es aditiva sobre V1 a V3 y no hay datos en producción.
- Para revertir antes de fusionar, se descarta la rama. Después, nunca se edita V4: se crea una migración nueva.
- Al fusionar: actualizar KB 04 (`prioridad`, `consultorio_id` nullable, tipo `RECUPERACION_CONTRASENA`) en el handoff.

## Open Questions

Ninguna bloquea C-05. Cada una tiene un valor por defecto aplicado en este diseño; las marcadas con (H) conviene que las confirme una persona en la revisión (governance ALTO).

1. **(H) Cantidad de intentos**: KB 11 y RN-NO-03 dicen "3 reintentos, a los 5, 15 y 60 minutos"; el Flujo 6 de KB 07 dice "a los 3 intentos la marca `FALLIDA`". *Por defecto*: 1 envío + 3 reintentos, `FALLIDA` con 4 intentos. Si se elige la lectura del Flujo 6, cambia una constante y un escenario.
2. **(H) Tipos inconsistentes entre KB 04 y KB 11**: KB 04 no tiene la recuperación de contraseña y KB 11 no tiene `OFERTA_TOMADA`. *Por defecto*: se incluyen los dos; `RECUPERACION_CONTRASENA` con prioridad 1 y `OFERTA_TOMADA` con prioridad 4 (informativo, se sacrifica primero ante la cuota). Se corrige la KB en el handoff.
3. **(H) Tick C-03/C-05** (D2): *por defecto*, C-05 entrega `TickRunner` y quien fusione segundo agrega la línea en el controller de C-03. Es una desviación de la letra de la ficha C-05; requiere el visto bueno de m1 (C-03) y m5.
4. **Alerta al `ADMIN_SAAS` por cuota agotada** (Flujo 6): un email no puede salir si la cuota se agotó. *Por defecto*: advertencia en el log una vez por día (con la cantidad pendiente); el aviso en el panel queda para C-23 (`admin-saas`, que ya muestra el consumo de la cuota).
5. **Notificaciones de un consultorio suspendido**: *por defecto* quedan `PENDIENTE`. Alternativa: marcarlas `FALLIDA` al suspender. Lo decide C-23 (`admin-saas`, que suspende y reactiva).
6. **Hora de reinicio de la cuota**: *por defecto* 00:00 UTC. Se revisa con el proveedor elegido en C-03 (PA-03).
7. **`consultorio_id` nullable en `notificacion`** (KB 04 no lo aclara): *por defecto* nullable, solo en modo sistema (D1).
