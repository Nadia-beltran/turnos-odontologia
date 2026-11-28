/**
 * Outbox de emails: encolado transaccional, plantillas, prioridades, cuota diaria y reintentos (RN-NO).
 *
 * <h2>Cómo encolar un email</h2>
 * <ol>
 *   <li>Llamar a {@link ar.turnos.notificaciones.NotificacionService#encolar} <b>dentro de la transacción del evento</b>
 *       que origina el email (reserva, cancelación, etc.). Nunca enviar un email directo desde un servicio (RN-NO-01):
 *       si la transacción se revierte, la notificación no existe.</li>
 *   <li>El consultorio sale del contexto de tenancy, no de un parámetro. Solo en modo sistema se puede encolar
 *       sin consultorio (p. ej. recuperación de contraseña del ADMIN_SAAS).</li>
 *   <li>El {@code payload} son las variables de la plantilla. <b>Nunca</b> DNI, datos clínicos ni tokens en claro
 *       (RN-SG-01, RN-SG-06): solo lo que el paciente debe ver en el email y los links con su token.</li>
 *   <li>Cada {@link ar.turnos.notificaciones.TipoNotificacion} necesita {@code templates/email/<tipo en minúsculas>.html}
 *       y {@code .txt}; si faltan, {@code encolar} falla nombrando el tipo. Un tipo nuevo también exige una migración que
 *       reemplace {@code notificacion_tipo_check}.</li>
 * </ol>
 *
 * <p>El envío lo hace después el procesador (ver {@code ProcesadorNotificaciones}), invocado por el tick.
 * Ver knowledge-base/08_arquitectura_propuesta.md (estructura de módulos) y 11_notificaciones_email.md.
 */
package ar.turnos.notificaciones;
