# Visión y Objetivos

## Propósito del sistema

**Reducir el ausentismo y recuperar los turnos perdidos de los odontólogos independientes que trabajan con un solo sillón.**

Hoy un turno cancelado tarde o un paciente que no se presenta son ingresos perdidos: el horario queda vacío porque no hay forma ágil de ofrecérselo a otro paciente. El sistema es un SaaS multi-tenant que combina:

- Reserva online sin cuenta.
- Recordatorios y gestión del turno por email.
- Una **lista de espera** que se activa automáticamente cuando se libera un horario.

La investigación de mercado (`discovery/research/analisis-competitivo.md`) no encontró ningún producto argentino que demuestre esta última función.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Odontólogo | Tener la agenda llena y sin superposiciones | Medir su ausentismo; dejar de coordinar turnos por teléfono o WhatsApp; llevar una ficha básica con odontograma |
| Paciente | Reservar y gestionar su turno sin llamar ni crear cuenta | Atenderse antes si se libera un turno |
| Recepción (si entra en el MVP) | Operar la agenda en nombre del odontólogo | Registrar asistencia y cargar sobreturnos |
| Administrador del SaaS | Que los consultorios se registren y operen solos | Controlar los límites de plan y el estado de cada consultorio |

## Alcance v1.0 (MVP)

- Registro de un consultorio con plan gratuito y aislamiento de datos por consultorio.
- **Agenda**:
  - Vistas diaria y semanal.
  - Duración por prestación.
  - Horarios de atención, bloqueos y feriados.
  - **Prevención de solapamientos**.
  - Sobreturno explícito cargado por el consultorio.
- **Reserva online** por link público del consultorio, sin cuenta: nombre, DNI, email y teléfono.
- **Emails transaccionales**: confirmación de reserva, recordatorio previo y link de gestión para confirmar, cancelar o reprogramar.
- Cancelación y reprogramación por parte del paciente **hasta 24 h antes** del turno.
- **Lista de espera**: el horario liberado se ofrece por email a todos los inscriptos y el primero que acepta se lo queda.
- Registro de asistencia (atendido o ausente) y **reporte de ausentismo**.
- Ficha básica del paciente con **odontograma** (FDI, adulto y pediátrico) e historial de cambios.
- Roles, **registro de auditoría** y **exportación** de los datos del consultorio.

## Fuera de alcance (v1.0)

- WhatsApp, SMS y notificaciones push.
- Señas y cobros online (Mercado Pago).
- Cobro automático de la suscripción del plan pago (pendiente de decisión, ver `10_preguntas_abiertas.md`).
- Facturación electrónica ARCA.
- Obras sociales y prepagas.
- Periodontograma, radiografías e imágenes.
- Recordatorio de control (recall) según la prestación realizada.
- Asistente conversacional con IA.
- Multisede y más de un sillón por consultorio.
- Aplicación móvil nativa: el frontend web debe ser *responsive*.

## Métricas de éxito

| Métrica | Definición | Meta inicial |
|---|---|---|
| Tasa de ausentismo | Turnos con estado AUSENTE / turnos vencidos (atendidos + ausentes), por consultorio y mes | Que baje respecto del primer mes de uso (línea base) |
| Recuperación de turnos | Turnos liberados reasignados por lista de espera / turnos liberados | Medir desde el lanzamiento; sin meta hasta tener datos |
| Autogestión del paciente | Turnos confirmados, cancelados o reprogramados por el paciente / total de turnos con email enviado | Medir |
| Adopción | Consultorios con al menos un turno en los últimos 30 días | Medir |
| Entregabilidad | Emails enviados sin rebote / emails intentados | ≥ 95 % |
