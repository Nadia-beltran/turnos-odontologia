package ar.turnos.notificaciones;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** Implementación SMTP del puerto: multiparte (texto y HTML) con la dirección de {@code app.mail.from}. */
@Component
class EnviadorSmtp implements EnviadorEmail {

    private final JavaMailSender sender;
    private final String direccionRemitente;

    EnviadorSmtp(JavaMailSender sender, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.direccionRemitente = direccion(from);
    }

    @Override
    public void enviar(EmailSaliente email) {
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            h.setFrom(new InternetAddress(direccionRemitente, email.nombreRemitente(), StandardCharsets.UTF_8.name()));
            h.setTo(email.destinatario());
            if (email.replyTo() != null) {
                h.setReplyTo(email.replyTo());
            }
            h.setSubject(email.asunto());
            h.setText(email.texto(), email.html());
            sender.send(mensaje);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("No se pudo armar el email", e);
        }
    }

    /** {@code app.mail.from} puede venir como "Nombre <dirección>": solo importa la dirección. */
    private static String direccion(String from) {
        try {
            return new InternetAddress(from).getAddress();
        } catch (MessagingException e) {
            throw new IllegalStateException("app.mail.from no es una dirección válida", e);
        }
    }
}
