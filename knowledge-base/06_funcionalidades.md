# Funcionalidades

Organizadas por épica y luego por historia de usuario (`US-NNN`). Todas son del MVP salvo que se indique otra cosa. La marca **[condicional PA-04]** indica que la historia depende de si la recepción entra en el MVP.

---

## Épica 1: Registro y acceso

### US-001 — Registrar mi consultorio
**Como** odontólogo **quiero** registrar mi consultorio con mis datos y un email **para** empezar a usar el plan gratuito sin hablar con nadie.

**Criterios de aceptación**:
- [ ] El formulario pide nombre del consultorio, nombre del odontólogo, email y contraseña.
- [ ] Se crean el consultorio, el usuario, el sillón, las prestaciones sugeridas y el horario por defecto en una sola operación.
- [ ] Se envía un email de verificación. Hasta verificarlo, la página pública no acepta reservas.
- [ ] Un email ya registrado devuelve un error genérico (no revela si la cuenta existe).

**Reglas relacionadas**: RN-AU-01, RN-AU-02, RN-AU-03, RN-AU-04, RN-AU-07

### US-002 — Verificar mi email
**Como** odontólogo **quiero** verificar mi email desde un link **para** activar mi consultorio.

**Criterios de aceptación**:
- [ ] El link vence a las 48 h y se puede pedir uno nuevo.
- [ ] Al verificar, el consultorio pasa a `ACTIVO`.

**Reglas relacionadas**: RN-AU-02, RN-SG-01

### US-003 — Iniciar y cerrar sesión
**Como** odontólogo o recepción **quiero** entrar con email y contraseña **para** acceder a mi panel.

**Criterios de aceptación**:
- [ ] La sesión usa una cookie `HttpOnly`, `Secure` y `SameSite=Lax`, con protección CSRF.
- [ ] Se aplica el bloqueo tras 5 intentos fallidos y el vencimiento por inactividad.

**Reglas relacionadas**: RN-AU-05, RN-AU-06

### US-004 — Recuperar mi contraseña
**Como** usuario **quiero** restablecer mi contraseña por email **para** volver a entrar si la olvido.

**Criterios de aceptación**:
- [ ] El link vence en 1 h y sirve una sola vez.
- [ ] La respuesta es la misma exista o no la cuenta.

### US-005 — Invitar a una recepcionista [condicional PA-04]
**Como** odontólogo **quiero** dar acceso a mi recepcionista con permisos reducidos **para** delegar la agenda.

**Criterios de aceptación**:
- [ ] La invitación por email crea un usuario `RECEPCION` cuando la persona define su contraseña.
- [ ] El odontólogo puede desactivar ese acceso.

---

## Épica 2: Configuración del consultorio

### US-010 — Configurar mis datos y mi link público
**Como** odontólogo **quiero** editar nombre, dirección, teléfono y slug **para** que mis pacientes me encuentren.

**Criterios de aceptación**:
- [ ] El slug se valida por formato y unicidad.
- [ ] Se muestra el link público listo para copiar.

### US-011 — Configurar mis horarios de atención
**Como** odontólogo **quiero** definir franjas por día de la semana **para** que solo se ofrezcan esos horarios.

**Criterios de aceptación**:
- [ ] Se admiten varias franjas por día, sin superposición entre ellas.
- [ ] Los cambios afectan solo a la disponibilidad futura; los turnos existentes no se tocan.

**Reglas relacionadas**: RN-AG-04

### US-012 — Gestionar mis prestaciones
**Como** odontólogo **quiero** crear prestaciones con su duración y decidir cuáles se reservan online **para** que cada turno dure lo que corresponde.

**Criterios de aceptación**:
- [ ] La duración va de 5 a 480 minutos, en múltiplos de 5.
- [ ] Desactivar una prestación no afecta a los turnos ya reservados.

**Reglas relacionadas**: RN-AG-03, RN-RE-02

### US-013 — Cargar bloqueos y feriados
**Como** odontólogo o recepción **quiero** bloquear días u horas **para** que no se reserven turnos cuando no atiendo.

**Criterios de aceptación**:
- [ ] Si el bloqueo se superpone con turnos activos, se avisa y se listan los turnos afectados. No se cancelan automáticamente.

**Reglas relacionadas**: RN-AG-06

### US-014 — Ajustar reglas de reserva y recordatorio
**Como** odontólogo **quiero** configurar la anticipación mínima, el horizonte de reserva y cuántas horas antes sale el recordatorio **para** adaptarlo a mi práctica.

**Criterios de aceptación**:
- [ ] El recordatorio sale como mínimo 25 h antes del turno.

**Reglas relacionadas**: RN-NO-02

---

## Épica 3: Agenda y turnos (consultorio)

### US-020 — Ver mi agenda diaria y semanal
**Como** odontólogo o recepción **quiero** ver los turnos del día y de la semana **para** organizar mi jornada.

**Criterios de aceptación**:
- [ ] Cada turno muestra paciente, prestación y estado.
- [ ] Los sobreturnos se distinguen visualmente, y también los turnos pasados pendientes de registrar.
- [ ] Se muestran los bloqueos y los horarios fuera de atención.
- [ ] La vista se adapta al celular.

**Reglas relacionadas**: RN-AG-02, RN-AS-01

### US-021 — Cargar un turno manualmente
**Como** odontólogo o recepción **quiero** dar un turno a un paciente que llama **para** centralizar todos los turnos en la agenda.

**Criterios de aceptación**:
- [ ] Se busca al paciente por DNI o apellido, o se lo crea.
- [ ] Si el horario se superpone con otro turno, la carga se rechaza y se ofrece marcarla como sobreturno.
- [ ] El paciente recibe el email de confirmación con su link de gestión.

**Reglas relacionadas**: RN-AG-01, RN-AG-02, RN-AG-07

### US-022 — Cancelar o reprogramar desde el consultorio
**Como** odontólogo o recepción **quiero** cancelar o mover un turno **para** reflejar cambios acordados con el paciente.

**Criterios de aceptación**:
- [ ] El paciente recibe el email correspondiente.
- [ ] El horario liberado dispara la lista de espera.

**Reglas relacionadas**: RN-TU-04, RN-TU-05, RN-TU-06

### US-023 — Registrar la asistencia
**Como** odontólogo o recepción **quiero** marcar si el paciente vino o no **para** medir el ausentismo.

**Criterios de aceptación**:
- [ ] Solo se puede marcar después de la hora de inicio.
- [ ] La marca se puede corregir, con auditoría.

**Reglas relacionadas**: RN-TU-01, RN-AS-01

---

## Épica 4: Reserva online (paciente)

### US-030 — Ver los horarios disponibles
**Como** paciente **quiero** elegir prestación, día y horario desde el link del consultorio **para** reservar sin llamar.

**Criterios de aceptación**:
- [ ] Se muestran solo las prestaciones reservables y los horarios que cumplen RN-AG-04.
- [ ] La página funciona bien en el celular.
- [ ] Si no hay horarios en el rango elegido, se ofrece inscribirse en la lista de espera.

**Reglas relacionadas**: RN-AG-04, RN-AG-05, RN-RE-02

### US-031 — Reservar un turno sin cuenta
**Como** paciente **quiero** reservar con mi nombre, DNI, email y teléfono **para** asegurar mi horario.

**Criterios de aceptación**:
- [ ] Se valida el DNI y el email.
- [ ] Se aplican el límite de turnos activos por DNI y la regla de coincidencia de apellido.
- [ ] Si el horario se ocupó mientras completaba el formulario, se informa y se ofrecen alternativas.
- [ ] Recibe un email con los datos del turno y el link de gestión.

**Reglas relacionadas**: RN-RE-01, RN-RE-03, RN-RE-04, RN-RE-05

---

## Épica 5: Gestión del turno por email (paciente)

### US-040 — Recibir la confirmación y el recordatorio
**Como** paciente **quiero** recibir un email al reservar y otro antes del turno **para** no olvidarme.

**Criterios de aceptación**:
- [ ] El recordatorio sale `horas_recordatorio` horas antes del turno.
- [ ] Ambos emails incluyen el link de gestión.

**Reglas relacionadas**: RN-NO-01, RN-NO-02

### US-041 — Confirmar mi asistencia
**Como** paciente **quiero** confirmar desde el link **para** avisarle al consultorio que voy.

**Criterios de aceptación**:
- [ ] El turno pasa a `CONFIRMADO`.
- [ ] Confirmar dos veces no da error.

**Reglas relacionadas**: RN-TU-01, RN-EX-01

### US-042 — Cancelar mi turno
**Como** paciente **quiero** cancelar desde el link hasta 24 h antes **para** liberar el horario.

**Criterios de aceptación**:
- [ ] Se pide confirmación antes de cancelar.
- [ ] Con menos de 24 h, no se ofrece la acción y se muestra el contacto del consultorio.
- [ ] Al cancelar, se dispara la lista de espera.

**Reglas relacionadas**: RN-TU-03, RN-TU-06

### US-043 — Reprogramar mi turno
**Como** paciente **quiero** elegir otro horario desde el link hasta 24 h antes **para** no perder mi lugar.

**Criterios de aceptación**:
- [ ] Se muestran horarios disponibles para la misma prestación.
- [ ] La operación es atómica.
- [ ] Recibe un email con el nuevo turno y un nuevo link.
- [ ] El horario original dispara la lista de espera.

**Reglas relacionadas**: RN-TU-03, RN-TU-05, RN-TU-06, RN-TU-07

---

## Épica 6: Lista de espera

### US-050 — Inscribirme en la lista de espera
**Como** paciente **quiero** anotarme indicando entre qué fechas puedo ir **para** enterarme si se libera un turno.

**Criterios de aceptación**:
- [ ] Una sola inscripción activa por consultorio.
- [ ] Recibe un email de inscripción con link de baja.

**Reglas relacionadas**: RN-LE-01, RN-LE-08

### US-051 — Recibir y aceptar un turno liberado
**Como** paciente en lista de espera **quiero** recibir un email cuando se libera un turno y tomarlo con un clic **para** atenderme antes.

**Criterios de aceptación**:
- [ ] El link muestra fecha, hora y prestación.
- [ ] El primero que acepta obtiene el turno confirmado y recibe su link de gestión.
- [ ] Los siguientes ven "ya fue tomado".
- [ ] Una oferta vencida muestra "esta oferta expiró".

**Reglas relacionadas**: RN-LE-02, RN-LE-03, RN-LE-05, RN-LE-06

### US-052 — Darme de baja de la lista
**Como** paciente **quiero** salir de la lista desde el email **para** no recibir más ofertas.

### US-053 — Ver la lista de espera
**Como** odontólogo o recepción **quiero** ver quiénes esperan y las ofertas enviadas **para** saber cuánta demanda tengo.

**Criterios de aceptación**:
- [ ] Se puede dar de baja a una inscripción.
- [ ] Se ve el estado de cada oferta: abierta, tomada o expirada.

---

## Épica 7: Pacientes y ficha clínica

### US-060 — Buscar y editar pacientes
**Como** odontólogo o recepción **quiero** buscar pacientes por DNI o apellido y corregir sus datos de contacto **para** mantenerlos al día.

**Criterios de aceptación**:
- [ ] Se muestran los turnos del paciente y su contador de ausencias.

**Reglas relacionadas**: RN-PA-01, RN-AS-03

### US-061 — Registrar el odontograma
**Como** odontólogo **quiero** marcar el estado de cada pieza y cara **para** llevar la ficha del paciente.

**Criterios de aceptación**:
- [ ] Notación FDI, con dentición permanente, temporaria o mixta.
- [ ] Cada cambio guarda autor y fecha.
- [ ] Se puede ver el historial por pieza.
- [ ] Una corrección no borra el registro anterior.

**Reglas relacionadas**: RN-CL-01, RN-CL-02, RN-CL-03

### US-062 — Escribir evoluciones
**Como** odontólogo **quiero** registrar notas de cada atención **para** tener el historial clínico.

**Criterios de aceptación**:
- [ ] La nota se puede asociar al turno.
- [ ] Las rectificaciones referencian la nota original.

**Reglas relacionadas**: RN-CL-01

---

## Épica 8: Reportes, auditoría y exportación

### US-070 — Ver mi ausentismo
**Como** odontólogo **quiero** ver la tasa de ausentismo y los turnos recuperados por lista de espera, por período **para** medir la mejora.

**Criterios de aceptación**:
- [ ] Se puede filtrar por mes o por rango de fechas.
- [ ] Se muestran: atendidos, ausentes, cancelados por paciente y por consultorio, liberados y reasignados.

**Reglas relacionadas**: RN-AS-02

### US-071 — Consultar la auditoría
**Como** odontólogo **quiero** ver quién hizo qué y cuándo en mi consultorio **para** tener trazabilidad.

**Reglas relacionadas**: RN-SG-02

### US-072 — Exportar mis datos
**Como** odontólogo **quiero** descargar todos mis datos **para** tener una copia o migrar a otro sistema.

**Criterios de aceptación**:
- [ ] Se descarga un ZIP con un CSV o JSON por entidad.
- [ ] La exportación queda auditada.

**Reglas relacionadas**: RN-SG-04

---

## Épica 9: Administración del SaaS

### US-080 — Administrar consultorios
**Como** administrador SaaS **quiero** listar consultorios, cambiar su plan y suspenderlos **para** operar el servicio.

**Criterios de aceptación**:
- [ ] No se muestran datos de pacientes.
- [ ] Toda acción queda auditada.

**Reglas relacionadas**: RN-MT-03, RN-PL-02, RN-SG-03

### US-081 — Ver métricas de la plataforma
**Como** administrador SaaS **quiero** ver consultorios activos, turnos por mes, emails enviados y fallidos, y el consumo de la cuota de email **para** anticipar los límites del plan gratuito.
