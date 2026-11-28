package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import ar.turnos.shared.AbstractIntegrationTest;
import ar.turnos.shared.MailpitClient;

/** Humo de la infraestructura de tests (design D8): JavaMailSender llega al Mailpit real y se puede vaciar. */
class MailpitHumoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JavaMailSender sender;
    @Autowired
    MailpitClient mailpit;

    @BeforeEach
    void limpiar() {
        mailpit.vaciar();
    }

    @Test
    void elEmailEnviadoApareceEnMailpit() {
        enviar("humo@example.test", "Asunto de humo");
        assertThat(mailpit.mensajes()).singleElement().satisfies(m -> {
            assertThat(m.get("Subject").asString()).isEqualTo("Asunto de humo");
            assertThat(m.get("To").get(0).get("Address").asString()).isEqualTo("humo@example.test");
        });
    }

    @Test
    void vaciarDejaLaBandejaSinMensajes() {
        enviar("otro@example.test", "Otro");
        mailpit.vaciar();
        assertThat(mailpit.mensajes()).isEmpty();
    }

    private void enviar(String para, String asunto) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom("no-reply@example.test");
        m.setTo(para);
        m.setSubject(asunto);
        m.setText("cuerpo");
        sender.send(m);
    }
}
