package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.turnos.shared.AbstractIntegrationTest;
import ar.turnos.shared.MailpitClient;
import tools.jackson.databind.JsonNode;

/** Spec email-notifications: email multiparte y remitente por consultorio, contra Mailpit real (design D7). */
class EnviadorSmtpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EnviadorEmail enviador;
    @Autowired
    MailpitClient mailpit;

    @BeforeEach
    void limpiar() {
        mailpit.vaciar();
    }

    private JsonNode unicoMensaje() {
        List<JsonNode> ms = mailpit.mensajes();
        assertThat(ms).hasSize(1);
        return ms.get(0);
    }

    @Test
    void emailMultiparteConHtmlYTextoPlano() {
        enviador.enviar(new EmailSaliente("paciente@example.test", "Consultorio Prueba vía Estudio Sonrisa", null,
                "Asunto", "<p>&lt;b&gt;Ana&lt;/b&gt;</p>", "<b>Ana</b>"));

        JsonNode m = unicoMensaje();
        assertThat(m.get("HTML").asString()).contains("&lt;b&gt;Ana&lt;/b&gt;");
        assertThat(m.get("Text").asString()).contains("<b>Ana</b>");
        assertThat(m.get("Subject").asString()).isEqualTo("Asunto");
        assertThat(m.get("To").get(0).get("Address").asString()).isEqualTo("paciente@example.test");
    }

    @Test
    void emailDeUnConsultorioTieneRemitenteVisibleYReplyTo() {
        enviador.enviar(new EmailSaliente("paciente@example.test", "Consultorio Prueba vía Estudio Sonrisa",
                "contacto@example.test", "Asunto", "<p>hola</p>", "hola"));

        JsonNode m = unicoMensaje();
        assertThat(m.get("From").get("Name").asString()).isEqualTo("Consultorio Prueba vía Estudio Sonrisa");
        assertThat(m.get("From").get("Address").asString()).isEqualTo("no-reply@localhost");
        assertThat(m.get("ReplyTo").get(0).get("Address").asString()).isEqualTo("contacto@example.test");
    }

    @Test
    void emailDelSistemaTieneSoloLaMarcaYNoTieneReplyTo() {
        enviador.enviar(new EmailSaliente("admin@example.test", "Estudio Sonrisa", null, "Asunto", "<p>hola</p>", "hola"));

        JsonNode m = unicoMensaje();
        assertThat(m.get("From").get("Name").asString()).isEqualTo("Estudio Sonrisa");
        assertThat(m.get("ReplyTo")).isNullOrEmpty();
    }
}
