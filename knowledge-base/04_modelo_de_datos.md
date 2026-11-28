# Modelo de Datos

## Convenciones

- **Multi-tenant con esquema compartido** (DD-01): toda tabla de negocio tiene `consultorio_id` (FK, `NOT NULL`, indexado). Hibernate aplica un filtro automático por el consultorio de la sesión y el código nunca arma consultas sin ese filtro (RN-MT-01).
- Claves primarias `UUID` (evitan identificadores enumerables en links y URLs).
- Fechas y horas en `timestamptz` (UTC en la base). La zona horaria de presentación es la del consultorio, por defecto `America/Argentina/Buenos_Aires`.
- Columnas de auditoría mínimas: `created_at`, `updated_at`. Las tablas con concurrencia crítica tienen `version` para bloqueo optimista.
- Los tokens enviados por email se guardan **solo como hash** (SHA-256), nunca en claro.

## Dominios

| Dominio | Entidades |
|---|---|
| Identidad y tenancy | `consultorio`, `usuario` |
| Configuración de agenda | `sillon`, `prestacion`, `horario_atencion`, `bloqueo` |
| Turnos | `turno`, `token_gestion` |
| Lista de espera | `inscripcion_espera`, `oferta_hueco`, `oferta_envio` |
| Pacientes y clínica | `paciente`, `odontograma_registro`, `evolucion_clinica` |
| Notificaciones | `notificacion` (outbox de emails) |
| Auditoría | `auditoria` |

## ERD

```
consultorio 1───* usuario
consultorio 1───* sillon (exactamente 1 en v1)
consultorio 1───* prestacion
consultorio 1───* horario_atencion
consultorio 1───* bloqueo
consultorio 1───* paciente ──1───* odontograma_registro
                     │      └─1───* evolucion_clinica *───0..1 turno
                     │
                     ├──1───* turno *───1 prestacion
                     │          │  *───1 sillon
                     │          │  0..1── turno (turno_origen_id, reprogramación)
                     │          └─1───* token_gestion
                     │
                     └──1───* inscripcion_espera *───0..1 prestacion

turno (cancelado o reprogramado) 1───0..1 oferta_hueco 1───* oferta_envio *───1 inscripcion_espera
oferta_hueco 0..1───1 turno (turno_asignado_id, al aceptarse)

consultorio 1───* notificacion *───0..1 turno
consultorio 1───* auditoria *───0..1 usuario
```

## Entidades

### consultorio (tenant)
- `id` UUID PK
- `nombre` varchar(120) NOT NULL
- `slug` varchar(60) NOT NULL **UNIQUE**: identificador del link público `/c/{slug}`
- `email_contacto` varchar(254) NOT NULL; `telefono` varchar(30); `direccion` varchar(200)
- `zona_horaria` varchar(50) NOT NULL DEFAULT `'America/Argentina/Buenos_Aires'`
- `plan` enum(`GRATUITO`, `PAGO`) NOT NULL DEFAULT `GRATUITO`
- `estado` enum(`PENDIENTE_VERIFICACION`, `ACTIVO`, `SUSPENDIDO`) NOT NULL
- `anticipacion_minima_reserva_min` int NOT NULL DEFAULT 60: antelación mínima para reservar online
- `horizonte_reserva_dias` int NOT NULL DEFAULT 60: hasta cuántos días adelante se puede reservar
- `horas_recordatorio` int NOT NULL DEFAULT 48: antelación con que se envía el recordatorio
- `created_at`, `updated_at`

### usuario
- `id` UUID PK
- `consultorio_id` UUID FK **NULL** solo si `rol = ADMIN_SAAS`
- `email` varchar(254) NOT NULL **UNIQUE** (normalizado a minúsculas)
- `password_hash` varchar(100) NOT NULL (BCrypt)
- `nombre` varchar(120) NOT NULL
- `rol` enum(`ADMIN_SAAS`, `ODONTOLOGO`, `RECEPCION`) NOT NULL
- `email_verificado` boolean NOT NULL DEFAULT false
- `activo` boolean NOT NULL DEFAULT true
- `ultimo_login_at` timestamptz
- CHECK: `(rol = 'ADMIN_SAAS') = (consultorio_id IS NULL)`

### sillon
- `id` UUID PK; `consultorio_id` FK; `nombre` varchar(60) NOT NULL DEFAULT `'Sillón 1'`; `activo` boolean
- v1: se crea uno automáticamente con el consultorio y no se pueden agregar más (DD-05).

### prestacion
- `id` UUID PK; `consultorio_id` FK
- `nombre` varchar(80) NOT NULL
- `duracion_min` int NOT NULL CHECK (`duracion_min` BETWEEN 5 AND 480 AND `duracion_min` % 5 = 0)
- `reservable_online` boolean NOT NULL DEFAULT true
- `activa` boolean NOT NULL DEFAULT true
- UNIQUE (`consultorio_id`, `nombre`)

### horario_atencion
- `id` UUID PK; `consultorio_id` FK
- `dia_semana` smallint NOT NULL CHECK 1..7 (ISO: 1 = lunes)
- `hora_inicio` time NOT NULL; `hora_fin` time NOT NULL; CHECK `hora_fin > hora_inicio`
- Admite varias franjas por día (por ejemplo, 9 a 13 y 15 a 19). Las franjas del mismo día no se superponen (validado en el servicio).

### bloqueo
- `id` UUID PK; `consultorio_id` FK
- `inicio` timestamptz NOT NULL; `fin` timestamptz NOT NULL; CHECK `fin > inicio`
- `tipo` enum(`FERIADO`, `VACACIONES`, `OTRO`) NOT NULL; `motivo` varchar(120)
- `creado_por` UUID FK usuario

### paciente
- `id` UUID PK; `consultorio_id` FK
- `dni` varchar(12) NOT NULL: normalizado, solo dígitos
- `nombre` varchar(80) NOT NULL; `apellido` varchar(80) NOT NULL
- `email` varchar(254) NOT NULL; `telefono` varchar(30) NOT NULL
- `fecha_nacimiento` date NULL (para el odontograma pediátrico)
- **UNIQUE (`consultorio_id`, `dni`)** (RN-PA-01)
- Índice: (`consultorio_id`, `apellido`, `nombre`) para búsqueda

### turno
- `id` UUID PK; `consultorio_id` FK; `sillon_id` FK; `paciente_id` FK; `prestacion_id` FK
- `inicio` timestamptz NOT NULL; `fin` timestamptz NOT NULL (= `inicio` + duración de la prestación al momento de reservar)
- `estado` enum(`RESERVADO`, `CONFIRMADO`, `CANCELADO`, `REPROGRAMADO`, `ATENDIDO`, `AUSENTE`) NOT NULL
- `origen` enum(`ONLINE`, `CONSULTORIO`, `LISTA_ESPERA`) NOT NULL
- `es_sobreturno` boolean NOT NULL DEFAULT false
- `turno_origen_id` UUID FK turno NULL: turno del que proviene por reprogramación
- `cancelado_por` enum(`PACIENTE`, `CONSULTORIO`, `SISTEMA`) NULL; `cancelado_at` timestamptz NULL
- `recordatorio_enviado_at` timestamptz NULL
- `version` int NOT NULL (bloqueo optimista)
- **Restricción de exclusión** (RN-AG-01), que garantiza la prevención de solapamientos en la propia base:
  ```sql
  CREATE EXTENSION IF NOT EXISTS btree_gist;
  ALTER TABLE turno ADD CONSTRAINT turno_sin_solapamiento
    EXCLUDE USING gist (
      sillon_id WITH =,
      tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado IN ('RESERVADO','CONFIRMADO','ATENDIDO','AUSENTE') AND NOT es_sobreturno);
  ```
- Índices: (`consultorio_id`, `inicio`); (`paciente_id`, `inicio`)

### token_gestion
- `id` UUID PK; `turno_id` FK
- `token_hash` char(64) NOT NULL UNIQUE
- `expira_at` timestamptz NOT NULL (= `turno.inicio`)
- `revocado_at` timestamptz NULL
- Un token activo por turno. Se reemplaza al reprogramar (el nuevo turno recibe uno nuevo).

### inscripcion_espera
- `id` UUID PK; `consultorio_id` FK; `paciente_id` FK
- `prestacion_id` FK NULL (NULL = cualquier prestación reservable)
- `fecha_desde` date NOT NULL; `fecha_hasta` date NOT NULL
- `estado` enum(`ACTIVA`, `ASIGNADA`, `CANCELADA`, `VENCIDA`) NOT NULL
- `token_baja_hash` char(64): para darse de baja desde el email
- UNIQUE parcial (`consultorio_id`, `paciente_id`) WHERE `estado = 'ACTIVA'`: una inscripción activa por paciente y consultorio

### oferta_hueco
- `id` UUID PK; `consultorio_id` FK; `sillon_id` FK
- `turno_liberado_id` FK turno NOT NULL UNIQUE: un turno liberado genera como máximo una oferta
- `inicio`, `fin` timestamptz NOT NULL; `prestacion_id` FK
- `estado` enum(`ABIERTA`, `TOMADA`, `EXPIRADA`, `ANULADA`) NOT NULL
- `turno_asignado_id` FK turno NULL
- `expira_at` timestamptz NOT NULL (RN-LE-05)
- `version` int NOT NULL: la aceptación concurrente se resuelve con bloqueo optimista y con la restricción de exclusión de `turno` (RN-LE-03)

### oferta_envio
- `id` UUID PK; `oferta_id` FK; `inscripcion_id` FK
- `token_hash` char(64) NOT NULL UNIQUE: token único por oferta y destinatario (SU-04)
- `enviado_at` timestamptz; `aceptado_at` timestamptz NULL

### odontograma_registro (append-only)
- `id` UUID PK; `consultorio_id` FK; `paciente_id` FK
- `pieza` smallint NOT NULL: código FDI (11–48 permanentes, 51–85 temporarias)
- `cara` enum(`OCLUSAL`, `MESIAL`, `DISTAL`, `VESTIBULAR`, `LINGUAL_PALATINA`, `PIEZA_COMPLETA`) NOT NULL
- `estado` enum: catálogo cerrado en v1 (ver PA-05). Propuesta: `SANO`, `CARIES`, `OBTURADO`, `AUSENTE`, `EXTRACCION_INDICADA`, `CORONA`, `ENDODONCIA`, `IMPLANTE`, `SELLADOR`
- `situacion` enum(`EXISTENTE`, `A_REALIZAR`, `REALIZADO`) NOT NULL
- `nota` varchar(500) NULL
- `registrado_por` UUID FK usuario NOT NULL; `registrado_at` timestamptz NOT NULL
- `reemplaza_id` UUID FK odontograma_registro NULL
- Estado vigente = último registro por (`paciente_id`, `pieza`, `cara`). No se hacen UPDATE ni DELETE (RN-CL-01).

### evolucion_clinica (append-only)
- `id` UUID PK; `consultorio_id` FK; `paciente_id` FK; `turno_id` FK NULL
- `texto` text NOT NULL; `autor_id` FK usuario; `created_at`
- `rectifica_id` FK evolucion_clinica NULL: una corrección referencia la evolución anterior, sin borrarla

### notificacion (outbox)
- `id` UUID PK; `consultorio_id` FK; `turno_id` FK NULL; `oferta_envio_id` FK NULL
- `tipo` enum(`VERIFICACION_EMAIL`, `RESERVA_CONFIRMADA`, `RECORDATORIO`, `TURNO_CANCELADO`, `TURNO_REPROGRAMADO`, `OFERTA_HUECO`, `OFERTA_TOMADA`, `INSCRIPCION_ESPERA`)
- `destinatario` varchar(254) NOT NULL; `payload` jsonb NOT NULL
- `estado` enum(`PENDIENTE`, `ENVIADA`, `FALLIDA`) NOT NULL; `intentos` smallint NOT NULL DEFAULT 0
- `programada_para` timestamptz NOT NULL; `enviada_at` timestamptz; `ultimo_error` text
- Índice: (`estado`, `programada_para`)

### auditoria (append-only)
- `id` bigserial PK; `consultorio_id` FK NULL; `usuario_id` FK NULL (NULL = paciente vía token o sistema)
- `actor_tipo` enum(`USUARIO`, `PACIENTE`, `SISTEMA`)
- `accion` varchar(60) NOT NULL (por ejemplo, `TURNO_CANCELADO`, `ODONTOGRAMA_REGISTRO`, `FICHA_LEIDA`, `EXPORTACION`)
- `entidad` varchar(40); `entidad_id` UUID; `datos` jsonb (estado previo o detalle)
- `ip` inet; `created_at` timestamptz NOT NULL
- Índice: (`consultorio_id`, `created_at` DESC)

## Seed data inicial

| Dato | Entorno | Detalle |
|---|---|---|
| Usuario `ADMIN_SAAS` | Todos | Se crea al arrancar a partir de variables de entorno, si no existe (ver 08) |
| Prestaciones sugeridas | Al crear un consultorio | Consulta (30 min), Limpieza (45), Arreglo/obturación (45), Extracción (60), Control (20). Editables |
| Sillón | Al crear un consultorio | `Sillón 1` |
| Horario por defecto | Al crear un consultorio | Lunes a viernes, de 9 a 18. Editable |
| Consultorio demo con pacientes y turnos ficticios | Solo desarrollo | Datos inventados, **nunca** datos reales |
