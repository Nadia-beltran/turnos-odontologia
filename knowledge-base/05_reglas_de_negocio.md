# Reglas de Negocio

Cada regla tiene un código único `RN-{DOMINIO}-{NN}` para trazabilidad. Las marcadas con **(Suposición)** salen de una inferencia, no de una decisión explícita, y figuran también en `09_decisiones_y_supuestos.md`.

## Dominio: Multi-tenancy (RN-MT)

- **RN-MT-01**: Todo dato de negocio pertenece a exactamente un consultorio. Un usuario `ODONTOLOGO` o `RECEPCION` solo lee y escribe datos de su consultorio. El filtro se aplica en la capa de persistencia, no en cada consulta manual.
- **RN-MT-02**: En las rutas públicas, el consultorio se resuelve **solo** por el `slug` (reserva e inscripción) o por el token (gestión y oferta). Nunca se acepta un `consultorio_id` enviado por el cliente.
- **RN-MT-03**: Un consultorio `SUSPENDIDO` no acepta reservas online ni envía emails. Sus usuarios pueden entrar en modo solo lectura y exportar sus datos.

## Dominio: Autenticación y cuentas (RN-AU)

- **RN-AU-01**: El alta de un consultorio crea el consultorio (estado `PENDIENTE_VERIFICACION`, plan `GRATUITO`), un usuario `ODONTOLOGO`, un sillón, las prestaciones sugeridas y el horario por defecto, en una única transacción.
- **RN-AU-02**: El consultorio pasa a `ACTIVO` cuando el odontólogo verifica su email. Hasta entonces su página pública no acepta reservas.
- **RN-AU-03**: El email de usuario es único en todo el sistema (normalizado a minúsculas).
- **RN-AU-04**: La contraseña tiene un mínimo de 10 caracteres y se guarda con BCrypt.
- **RN-AU-05**: Tras 5 intentos de login fallidos consecutivos, la cuenta se bloquea 15 minutos. **(Suposición)**
- **RN-AU-06**: La sesión vence tras 8 h de inactividad. **(Suposición)**
- **RN-AU-07**: El slug del consultorio es único, en minúsculas, con letras, números y guiones, de 3 a 60 caracteres. Se sugiere a partir del nombre y se puede editar.

## Dominio: Agenda (RN-AG)

- **RN-AG-01**: Un sillón no puede tener dos turnos activos (`RESERVADO`, `CONFIRMADO`, `ATENDIDO`, `AUSENTE`) con horarios superpuestos, **salvo** que uno sea sobreturno. Lo garantiza la restricción de exclusión de la base de datos, además de la validación previa en el servicio.
- **RN-AG-02**: Solo el consultorio (`ODONTOLOGO` o `RECEPCION`) puede cargar un **sobreturno**. El sobreturno queda marcado, se muestra distinto en la agenda y nunca se ofrece online.
- **RN-AG-03**: La duración de un turno es la de la prestación **en el momento de reservar**. Si después se cambia la duración de la prestación, los turnos existentes no cambian.
- **RN-AG-04**: Un horario está **disponible** para reserva online si cumple todas estas condiciones:
  1. Está dentro de una franja de `horario_atencion`.
  2. No se superpone con un bloqueo.
  3. No se superpone con un turno activo.
  4. Empieza al menos `anticipacion_minima_reserva_min` minutos después del momento actual.
  5. Está dentro de `horizonte_reserva_dias`.
  6. Entra completo antes del fin de la franja.
- **RN-AG-05**: Los inicios de turno se calculan en pasos de 15 minutos desde el inicio de la franja. **(Suposición)**
- **RN-AG-06**: Crear un bloqueo que se superpone con turnos activos **no** los cancela automáticamente: el sistema avisa y lista los turnos afectados para que el consultorio decida. **(Suposición)**
- **RN-AG-07**: El consultorio sí puede cargar turnos fuera del horario de atención (no así el paciente online).

## Dominio: Turnos (RN-TU)

- **RN-TU-01**: Estados y transiciones válidas:

  | Desde | Hacia | Quién |
  |---|---|---|
  | (nuevo) | `RESERVADO` | Paciente online, consultorio o lista de espera |
  | `RESERVADO` | `CONFIRMADO` | Paciente (token) o consultorio |
  | `RESERVADO`, `CONFIRMADO` | `CANCELADO` | Paciente (RN-TU-03) o consultorio |
  | `RESERVADO`, `CONFIRMADO` | `REPROGRAMADO` | Paciente (RN-TU-03) o consultorio |
  | `RESERVADO`, `CONFIRMADO` | `ATENDIDO`, `AUSENTE` | Consultorio, solo después de la hora de inicio |
  | `ATENDIDO` ↔ `AUSENTE` | (corrección) | Consultorio, con auditoría |

  `CANCELADO` y `REPROGRAMADO` son estados finales.
- **RN-TU-02**: Un turno sin confirmar **no** se cancela automáticamente en v1. Sigue siendo `RESERVADO` y cuenta como ocupado. **(Suposición)**
- **RN-TU-03**: El paciente puede **cancelar o reprogramar** por su cuenta solo hasta **24 h antes** del inicio del turno. Pasado ese plazo, el link de gestión muestra el turno y los datos de contacto del consultorio, sin esas acciones. La confirmación sí se permite hasta el inicio.
- **RN-TU-04**: El consultorio puede cancelar o reprogramar en cualquier momento antes del inicio del turno.
- **RN-TU-05**: **Reprogramar** es una operación atómica: se crea el turno nuevo (con `turno_origen_id`) y el original pasa a `REPROGRAMADO`. Si el horario nuevo ya no está disponible, la operación falla y el turno original queda intacto.
- **RN-TU-06**: **Todo turno que pasa a `CANCELADO` o `REPROGRAMADO` libera su horario y dispara la lista de espera** (RN-LE-02), quien sea que lo haya cancelado.
- **RN-TU-07**: Al reprogramar se revoca el token de gestión del turno original y se emite uno nuevo para el turno nuevo.
- **RN-TU-08**: Los turnos nunca se eliminan físicamente.

## Dominio: Reserva online (RN-RE)

- **RN-RE-01**: La reserva online pide **nombre, apellido, DNI, email y teléfono**. No requiere cuenta.
- **RN-RE-02**: Solo se pueden reservar online las prestaciones con `reservable_online = true`.
- **RN-RE-03**: Un DNI no puede tener más de **2 turnos futuros activos** reservados online en el mismo consultorio. Evita el acaparamiento de horarios. **(Suposición)**
- **RN-RE-04**: Si el DNI ya existe en el consultorio y el apellido coincide (comparación normalizada, sin tildes ni mayúsculas), se actualizan el email y el teléfono del paciente. Si el apellido **no** coincide, la reserva se rechaza con el mensaje "contactá al consultorio". Así se evita que alguien cambie los datos de contacto de otra persona. **(Suposición)**
- **RN-RE-05**: La disponibilidad se vuelve a verificar al confirmar la reserva. Si el horario se ocupó mientras el paciente completaba el formulario, se informa y se ofrecen otros horarios.

## Dominio: Lista de espera (RN-LE)

- **RN-LE-01**: Un paciente se inscribe con sus datos (como en RN-RE-01), un rango de fechas (`fecha_desde` a `fecha_hasta`, como máximo dentro de `horizonte_reserva_dias`) y, opcionalmente, una prestación. Puede tener **una inscripción activa por consultorio**.
- **RN-LE-02**: Cuando un turno se libera (RN-TU-06) y el horario empieza más tarde que el momento actual más `anticipacion_minima_reserva_min`, se crea una **oferta** y se envía por email a **todas** las inscripciones activas que cumplan las tres condiciones siguientes:
  - El día del horario está dentro de su rango.
  - Su prestación (o la prestación liberada, si no eligió ninguna) entra en la duración del horario.
  - El paciente no es el mismo que liberó el turno.
- **RN-LE-03**: **El primero que acepta se queda con el turno.** La aceptación crea el turno (origen `LISTA_ESPERA`, estado `CONFIRMADO`) y pasa la oferta a `TOMADA` en una única transacción. Las aceptaciones concurrentes las resuelven el bloqueo optimista de la oferta y la restricción de exclusión de turnos. El resto ve "este turno ya fue tomado".
- **RN-LE-04**: Al aceptar, la inscripción pasa a `ASIGNADA`. Las demás inscripciones notificadas siguen `ACTIVA`.
- **RN-LE-05**: La oferta **expira** cuando falta menos de `anticipacion_minima_reserva_min` para su inicio. Si nadie la acepta, pasa a `EXPIRADA` y el horario queda libre para reserva online común.
- **RN-LE-06**: El turno asignado usa la prestación de la inscripción (o la liberada si la inscripción no tiene una) y empieza a la hora de inicio del horario liberado.
- **RN-LE-07**: Las inscripciones cuyo `fecha_hasta` ya pasó pasan a `VENCIDA` mediante una tarea programada.
- **RN-LE-08**: Cada email de oferta y de inscripción incluye un link para darse de baja de la lista.

## Dominio: Pacientes (RN-PA)

- **RN-PA-01**: Un paciente se identifica por **DNI dentro de cada consultorio**. El mismo DNI en dos consultorios corresponde a dos registros independientes.
- **RN-PA-02**: El DNI se guarda normalizado: solo dígitos, entre 7 y 9 caracteres. **(Suposición)**: alcanza para DNI argentinos; los pacientes extranjeros quedan como pregunta abierta.

## Dominio: Ficha clínica (RN-CL)

- **RN-CL-01**: El odontograma y las evoluciones son **append-only**. No se editan ni se borran. Una corrección crea un registro nuevo que referencia al anterior. Esto respeta la inalterabilidad de la historia clínica que exige la Ley 26.529.
- **RN-CL-02**: Solo `ODONTOLOGO` crea y lee la ficha clínica. Cada lectura de una ficha queda en la auditoría (`FICHA_LEIDA`).
- **RN-CL-03**: El odontograma muestra la dentición permanente (FDI 11–48), la temporaria (FDI 51–85) o la mixta, según lo elija el odontólogo. La edad del paciente solo sugiere una por defecto.

## Dominio: Asistencia y reportes (RN-AS)

- **RN-AS-01**: El consultorio marca `ATENDIDO` o `AUSENTE` después de la hora de inicio del turno. Los turnos pasados sin marcar aparecen como "pendiente de registrar".
- **RN-AS-02**: **Tasa de ausentismo** = `AUSENTE` / (`ATENDIDO` + `AUSENTE`) en el período. Los turnos cancelados y los pendientes de registrar no cuentan.
- **RN-AS-03**: Una ausencia **solo queda registrada**, sin penalidad para el paciente. Se muestra el contador de ausencias en la ficha de contacto del paciente.

## Dominio: Notificaciones (RN-NO)

- **RN-NO-01**: Todos los emails se encolan en la tabla `notificacion`, dentro de la misma transacción del evento que los origina (patrón outbox), y los envía una tarea programada.
- **RN-NO-02**: El recordatorio se envía `horas_recordatorio` horas antes del turno (48 h por defecto, mínimo 25 h), para que el paciente todavía pueda cancelar por su cuenta. Si el turno se reservó con menos antelación, no se envía recordatorio: solo la confirmación de reserva, que ya incluye el link de gestión.
- **RN-NO-03**: Un email fallido se reintenta hasta 3 veces con espera creciente. Después queda `FALLIDA` y se muestra en el panel.
- **RN-NO-04**: Detalles de envío, plantillas y límites diarios: ver `11_notificaciones_email.md`.

## Dominio: Planes (RN-PL)

- **RN-PL-01**: El plan `GRATUITO` tiene límites (valores pendientes, PA-01). Al superar el límite mensual de turnos, la página pública deja de aceptar reservas y muestra el contacto del consultorio. El consultorio puede seguir cargando turnos a mano, y el panel muestra un aviso.
- **RN-PL-02**: En v1, el cambio de plan lo hace manualmente el `ADMIN_SAAS` (el cobro automático de suscripciones está fuera de alcance, PA-02).

## Dominio: Seguridad y datos personales (RN-SG)

- **RN-SG-01**: Los tokens de los links (gestión, oferta, baja, verificación) tienen al menos 128 bits aleatorios, viajan solo en la URL del email y se guardan como hash SHA-256.
- **RN-SG-02**: Toda acción que modifica turnos, pacientes, ficha clínica o configuración queda en `auditoria`, con actor, fecha, IP y estado previo.
- **RN-SG-03**: `ADMIN_SAAS` no accede a datos de pacientes ni a fichas clínicas. Solo a datos del consultorio y a métricas agregadas.
- **RN-SG-04**: El odontólogo puede **exportar todos los datos de su consultorio** (pacientes, turnos, ficha clínica y auditoría) en cualquier momento, incluso con el consultorio suspendido. La exportación también queda auditada.
- **RN-SG-05**: Los endpoints públicos tienen límite de tasa por IP: reservas e inscripciones, como máximo 10 por hora por IP; consultas de disponibilidad, 60 por minuto. **(Suposición)**
- **RN-SG-06**: Los emails y las páginas públicas nunca muestran datos clínicos. El link de gestión muestra solo fecha, hora, prestación y datos del consultorio.

## Dominio: Excepciones globales

- **RN-EX-01**: Todas las operaciones que cambian el estado de un turno u oferta son transaccionales e idempotentes ante reintentos del mismo token. Por ejemplo, confirmar dos veces deja el turno confirmado y no genera error.
- **RN-EX-02**: Los horarios se muestran y se ingresan en la zona horaria del consultorio, y se guardan en UTC.
