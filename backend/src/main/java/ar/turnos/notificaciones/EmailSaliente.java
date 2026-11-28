package ar.turnos.notificaciones;

/**
 * Un email listo para enviar.
 *
 * @param nombreRemitente nombre visible: "{consultorio} vía {marca}", o solo la marca (DD-14)
 * @param replyTo         email de contacto del consultorio; {@code null} para emails del sistema
 */
public record EmailSaliente(String destinatario, String nombreRemitente, String replyTo, String asunto, String html,
        String texto) {
}
