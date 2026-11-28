# Discovery: Turnos Odontología

**Fecha**: 2026-11-28
**Fuentes investigadas**: 18 sistemas leídos en sus sitios oficiales (ver `discovery/research/analisis-competitivo.md`)
**Estado**: confirmado por la usuaria el 2026-11-28

## 1. Problema que resuelve

Los odontólogos independientes que trabajan con un solo sillón pierden ingresos por **ausentismo** y por **turnos que quedan vacíos** cuando un paciente cancela tarde o no avisa. Hoy ese horario liberado no se reaprovecha.

## 2. Usuarios y roles

- **Odontólogo** (cliente del SaaS): gestiona su agenda y registra la asistencia.
- **Paciente**: reserva, confirma, cancela o reprograma su turno sin crear cuenta.
- **Recepción** (opcional): opera la agenda en nombre del odontólogo. Su inclusión en el MVP es una pregunta abierta.
- **Administrador del SaaS**: da de alta y gestiona los consultorios.

## 3. Casos de uso

1. Como paciente, quiero reservar un turno online sin crear cuenta, para no tener que llamar al consultorio.
2. Como paciente, quiero confirmar, cancelar o reprogramar mi turno desde el email hasta 24 h antes, para avisar sin llamar.
3. Como paciente en lista de espera, quiero recibir un aviso por email cuando se libera un turno, para atenderme antes.
4. Como odontólogo, quiero ver mi agenda diaria y semanal sin superposiciones, para organizar mi jornada.
5. Como odontólogo, quiero registrar quién asistió y ver mi tasa de ausentismo, para medir el problema y su evolución.

## 4. Competidores y soluciones existentes

El detalle completo, con tabla comparativa, matriz ponderada y fuentes, está en `discovery/research/analisis-competitivo.md`.

| Competidor | Foco | Precio publicado (28/11/2026) | Relevancia para este producto |
|---|---|---|---|
| DentalSoft (AR) | Ciclo de turnos completo y odontograma | Plan gratuito; $30.000 y $60.000/mes | Competidor directo; mejor puntaje de evidencia (4,10) |
| Dentalink (CL/AR) | Clínica profunda, obras sociales | No publicado | Líder regional; apunta a clínicas más grandes |
| AgendaPro (LatAm) | Canal digital del paciente, señas, IA | $9.900 a $129.900/mes | Referencia de reserva online; sin odontograma nativo |
| Turnito (AR) | Reserva sin cuenta, señas | Gratis sin vencimiento | Competencia en precio y referencia de experiencia de usuario |
| Odonto.ar (AR) | Odontograma interactivo, simplicidad | USD 29 a 99/mes | Mismo segmento (profesional solo); referencia de experiencia de usuario |

**Notas**:
- Ningún producto argentino evidencia una **lista de espera** que reaproveche turnos cancelados. Es el diferenciador principal del MVP.
- Los planes gratuitos de Turnito y DentalSoft fijan el piso de precio del mercado.
- Doctoralia Argentina, ZYRA y Reservo quedaron sin evaluar y requieren revisión manual.

## 5. Funcionalidades necesarias (MVP)

- **Agenda**:
  - Vistas diaria y semanal.
  - Duración por prestación.
  - Bloqueos y feriados.
  - **Prevención de solapamientos**.
  - Sobreturno solo cargado por el consultorio.
- **Reserva online** por link, sin cuenta, con nombre, DNI, email y teléfono.
- **Recordatorio por email** con acciones para confirmar, cancelar y reprogramar.
- **Lista de espera** con aviso por email cuando se libera un turno.
- **Registro de asistencia** y reporte de ausentismo.
- Ficha básica del paciente con **odontograma**.
- Roles, registro de auditoría y exportación de datos.
- **Multi-tenant**: varios consultorios en el mismo sistema, cada uno con sus datos aislados.
- Plan gratuito con límites.

## 6. Funcionalidades opcionales (después del MVP)

- Seña con Mercado Pago.
- Recordatorios y gestión del turno por WhatsApp.
- Recordatorio de control según la prestación realizada.
- Plan pago con cobro de suscripción (su entrada en el MVP es una pregunta abierta).
- Facturación electrónica ARCA.
- Obras sociales y prepagas.
- Periodontograma, radiografías e imágenes.
- Asistente conversacional con IA.
- Multisede.

## 7. Reglas de negocio

- El paciente puede cancelar o reprogramar por su cuenta **hasta 24 h antes** del turno. Pasado ese plazo, solo puede hacerlo el consultorio.
- **Un turno cancelado libera su horario y dispara el aviso a la lista de espera**, cancele el paciente o el consultorio.
- Una reprogramación libera el horario original y dispara la lista de espera de la misma forma.
- Un sillón no puede tener dos turnos a la misma hora, salvo un sobreturno cargado explícitamente por el consultorio.
- Una ausencia **solo queda registrada**, sin penalidad para el paciente.
- Cada paciente se identifica por su **DNI dentro de cada consultorio**.

## 8. Integraciones

- Proveedor de envío de emails transaccionales, en su plan gratuito.
- Fuera del MVP: Mercado Pago, WhatsApp y ARCA.

## 9. Restricciones

- **Stack obligatorio**:
  - Frontend en TypeScript sin framework.
  - Backend en Spring Boot con API REST.
  - Gradle, Hibernate y PostgreSQL.
- **Sin presupuesto**: hosting, base de datos y envío de emails deben funcionar en planes gratuitos.
- **Producto comercial**: SaaS multi-tenant para vender a odontólogos independientes con un sillón.
- **Normativa**: datos de salud alcanzados por la Ley 25.326 (protección de datos personales) y la Ley 26.529 (derechos del paciente e historia clínica).

## 10. Riesgos

- **Supuesto sin probar**: que el email baje el ausentismo de forma comparable a WhatsApp, que es el canal que usan los competidores.
- **Riesgo**: los límites de envío diario de los planes gratuitos de email y la posibilidad de que los mensajes caigan en spam.
- **Riesgo**: el hosting gratuito para Spring Boot (memoria limitada, arranques lentos) puede degradar la experiencia del paciente al reservar.
- **Riesgo**: competir contra planes gratuitos ya establecidos (Turnito, DentalSoft).
- **Riesgo**: cumplimiento normativo al tratar datos sensibles (DNI, historia clínica) sin presupuesto para asesoría legal.

## 11. Preguntas abiertas

- ¿Cuáles son los límites del plan gratuito y el precio del plan pago?
- ¿El cobro de la suscripción entra en el MVP, o se lanza primero solo el plan gratuito?
- ¿Cómo se asigna un turno liberado: al primero que acepta, o por orden de la lista de espera?
- ¿La recepción entra en el MVP?
- ¿Qué nivel de detalle tiene el odontograma de la primera versión?
- ¿Qué hosting y qué proveedor de email gratuitos se usan?
