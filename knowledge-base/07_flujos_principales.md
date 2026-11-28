# Flujos Principales

Componentes: **FE** = frontend TS · **Proxy** = reenvío `/api` del hosting estático · **API** = backend Spring Boot · **DB** = PostgreSQL · **Tarea** = tarea programada del backend · **Email** = proveedor de email.

## Flujo 1: Registro y verificación del consultorio

**Disparador**: un odontólogo envía el formulario de registro.
**Actor**: odontólogo.

**Pasos**:
1. FE envía `POST /api/auth/registro` con los datos del consultorio, nombre, email y contraseña.
2. La API valida los datos y, en una transacción, crea el consultorio (`PENDIENTE_VERIFICACION`), el usuario `ODONTOLOGO`, el sillón, las prestaciones sugeridas y el horario por defecto. También encola una notificación `VERIFICACION_EMAIL` con un token.
3. La Tarea envía el email.
4. El odontólogo abre el link `/verificar-email?token=…`, y FE llama a `POST /api/auth/verificar-email`.
5. La API valida el hash del token y su vencimiento, marca el email como verificado y pasa el consultorio a `ACTIVO`.

**Casos de error**:
- Email ya registrado → respuesta genérica, sin revelar la existencia de la cuenta.
- Token vencido → se ofrece reenviar el email.

---

## Flujo 2: Reserva online

**Disparador**: un paciente abre `/c/{slug}`.
**Actor**: paciente.

**Pasos**:
1. FE pide `GET /api/public/c/{slug}` y `.../prestaciones`.
2. El paciente elige una prestación. FE pide `.../disponibilidad?prestacion=&desde=&hasta=`.
3. La API calcula los horarios libres aplicando RN-AG-04 y RN-AG-05 sobre los horarios de atención, los bloqueos y los turnos activos.
4. El paciente elige un horario y completa nombre, apellido, DNI, email y teléfono.
5. FE envía `POST /api/public/c/{slug}/reservas`.
6. La API, en una transacción:
   1. Aplica el límite de tasa por IP.
   2. Resuelve el consultorio por el slug y valida que esté `ACTIVO` y dentro del límite del plan.
   3. Busca o crea el paciente por DNI, aplicando RN-RE-04.
   4. Valida el límite de turnos activos por DNI (RN-RE-03).
   5. Inserta el turno `RESERVADO`. La restricción de exclusión rechaza el insert si el horario se ocupó.
   6. Genera el token de gestión y guarda su hash.
   7. Encola `RESERVA_CONFIRMADA` y, si corresponde, `RECORDATORIO` con `programada_para = inicio − horas_recordatorio`.
   8. Registra la acción en la auditoría.
7. La API responde 201 y FE muestra la confirmación.

```
Paciente → FE → Proxy → API ──► DB (INSERT turno; EXCLUDE valida el solapamiento)
                          │       └─ INSERT notificacion (misma transacción)
                          ◄─ 201
           Tarea ─► DB (notificaciones PENDIENTE) ─► Email ─► Paciente
```

**Casos de error**:
- Violación de la restricción de exclusión → 409 "el horario ya no está disponible", con horarios alternativos.
- Apellido que no coincide con el DNI → 422 "contactá al consultorio".
- Límite del plan alcanzado o consultorio suspendido → la página muestra el contacto del consultorio.
- Límite de tasa superado → 429.

---

## Flujo 3: Recordatorio y gestión del turno por el paciente

**Disparador**: llega el momento de `programada_para` del recordatorio, o el paciente abre su link.
**Actor**: sistema y paciente.

**Pasos**:
1. La Tarea (cada 5 minutos) toma las notificaciones `PENDIENTE` vencidas y descarta los recordatorios de turnos que ya no están activos. Envía las demás y marca `recordatorio_enviado_at`.
2. El paciente abre `/t/{token}`. FE pide `GET /api/public/turnos/{token}`.
3. La API busca el hash, valida que el token no esté vencido ni revocado y devuelve fecha, hora, prestación, datos del consultorio y las acciones permitidas. Cancelar y reprogramar se permiten solo si faltan más de 24 h (RN-TU-03).
4. El paciente elige una acción:
   - **Confirmar** → `POST .../confirmar` → estado `CONFIRMADO` (idempotente).
   - **Cancelar** → `POST .../cancelar` → estado `CANCELADO`, `cancelado_por = PACIENTE`. Se encola `TURNO_CANCELADO` y se publica el evento `TurnoLiberado` (Flujo 4).
   - **Reprogramar** → FE pide disponibilidad para la misma prestación y luego `POST .../reprogramar {nuevoInicio}`. En una transacción: se inserta el turno nuevo, el original pasa a `REPROGRAMADO`, se revoca el token viejo, se emite uno nuevo, se encola `TURNO_REPROGRAMADO` y se publica `TurnoLiberado` para el horario original.

**Casos de error**:
- Token inválido o revocado → página "link no válido", con el contacto del consultorio si se lo puede resolver.
- Faltan menos de 24 h → 409, y FE muestra el contacto.
- Horario nuevo ocupado → 409, y el turno original queda intacto.

---

## Flujo 4: Turno liberado y lista de espera

**Disparador**: el evento `TurnoLiberado`, publicado al cancelar o reprogramar (por el paciente o por el consultorio).
**Actor**: sistema y pacientes en lista de espera.

**Pasos**:
1. Un *listener* que corre **después del commit** de la transacción del turno recibe el evento.
2. Si el horario empieza después del momento actual más `anticipacion_minima_reserva_min`, crea una `oferta_hueco` (`ABIERTA`, con `expira_at` según RN-LE-05).
3. Busca las inscripciones `ACTIVA` que cumplen RN-LE-02.
4. Por cada inscripción crea un `oferta_envio` con su token (se guarda solo el hash) y encola `OFERTA_HUECO`.
5. Si no hay inscripciones compatibles, la oferta queda `EXPIRADA` de inmediato y el horario vuelve a la disponibilidad online.
6. Un paciente abre `/o/{token}` → `GET /api/public/ofertas/{token}`, que devuelve la oferta y su estado.
7. El paciente acepta → `POST /api/public/ofertas/{token}/aceptar`. En una transacción:
   1. Se lee la oferta con su `version` y se valida que esté `ABIERTA` y no vencida.
   2. Se inserta el turno (`LISTA_ESPERA`, `CONFIRMADO`). La restricción de exclusión impide un doble turno.
   3. La oferta pasa a `TOMADA` (UPDATE con control de `version`) y se le asigna `turno_asignado_id`.
   4. La inscripción del ganador pasa a `ASIGNADA`.
   5. Se genera el token de gestión y se encola `RESERVA_CONFIRMADA`.
8. Si dos pacientes aceptan a la vez, uno hace commit. El otro recibe un conflicto de versión o de exclusión, y la API responde 409 "este turno ya fue tomado".
9. La Tarea pasa a `EXPIRADA` las ofertas vencidas.

```
Cancelación ─commit─► TurnoLiberado ─► Listener ─► oferta_hueco + N oferta_envio + N notificacion
                                                              │
Paciente A ─aceptar─┐                                         ▼
Paciente B ─aceptar─┴─► API ─► transacción: INSERT turno + UPDATE oferta (version)
                               ├─ A: commit → 200, turno confirmado
                               └─ B: OptimisticLock / Exclusion → 409 "ya fue tomado"
```

**Casos de error**:
- Oferta expirada → 410 "esta oferta expiró".
- Inscripción dada de baja después del envío → 410.
- El horario se ocupó por reserva online o sobreturno antes de que alguien acepte → la exclusión rechaza el insert, 409, y la oferta pasa a `ANULADA`.

> **Nota**: mientras una oferta está `ABIERTA`, el horario **sigue disponible** para la reserva online común. Gana quien llegue primero, por cualquiera de los dos canales. **(Suposición SU-05)**

---

## Flujo 5: Registro de asistencia

**Disparador**: el consultorio revisa la agenda después de un turno.
**Actor**: odontólogo o recepción.

**Pasos**:
1. FE muestra los turnos pasados con estado `RESERVADO` o `CONFIRMADO` como "pendiente de registrar".
2. El usuario marca el turno. FE envía `PATCH /api/turnos/{id}/estado {ATENDIDO|AUSENTE}`.
3. La API valida que la hora de inicio ya pasó, actualiza el estado y lo registra en la auditoría.

**Casos de error**:
- Intento de marcar antes del inicio → 409.

---

## Flujo 6: Procesamiento de la cola de emails (outbox)

**Disparador**: la tarea programada, cada 5 minutos, o la llamada externa de *keep-alive* (ver `12_despliegue_y_entornos.md`).
**Actor**: sistema.

**Pasos**:
1. Selecciona hasta N notificaciones `PENDIENTE` con `programada_para <= ahora`, ordenadas por prioridad y por fecha, con `FOR UPDATE SKIP LOCKED`.
2. Verifica la cuota diaria restante del proveedor de email (ver `11_notificaciones_email.md`).
3. Envía cada email y la marca `ENVIADA`, o incrementa `intentos` y reprograma con espera creciente. A los 3 intentos la marca `FALLIDA`.
4. Registra métricas de envío.

**Casos de error**:
- Proveedor caído → reintentos. Las notificaciones no se pierden porque viven en la base.
- Cuota diaria agotada → las notificaciones quedan `PENDIENTE` hasta el día siguiente, por orden de prioridad, y se alerta al `ADMIN_SAAS`.
