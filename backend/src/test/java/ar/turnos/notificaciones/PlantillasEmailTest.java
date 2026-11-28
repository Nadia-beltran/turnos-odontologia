package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Spec email-notifications: contenido HTML y texto plano, escapado y sin imágenes remotas (design D6). */
class PlantillasEmailTest {

    private final PlantillasEmail plantillas = new PlantillasEmail("Estudio Sonrisa", "https://app.example.test");

    @Test
    void elHtmlEscapaYElTextoPlanoNo() {
        ContenidoEmail c = plantillas.render(TipoNotificacion.RESERVA_CONFIRMADA, Map.of("nombre", "<b>Ana</b>"), "Consultorio Prueba");

        assertThat(c.html()).contains("&lt;b&gt;Ana&lt;/b&gt;").doesNotContain("<b>Ana</b>");
        assertThat(c.texto()).contains("<b>Ana</b>").doesNotContain("&lt;");
    }

    @Test
    void inyectaConsultorioMarcaYBaseUrl() {
        ContenidoEmail c = plantillas.render(TipoNotificacion.RECORDATORIO, Map.of("nombre", "Luis"), "Consultorio Prueba");

        assertThat(c.html()).contains("Consultorio Prueba vía Estudio Sonrisa").contains("https://app.example.test");
        assertThat(c.texto()).contains("Consultorio Prueba vía Estudio Sonrisa").contains("https://app.example.test");
    }

    @Test
    void sinConsultorioElPieEsSoloLaMarca() {
        ContenidoEmail c = plantillas.render(TipoNotificacion.VERIFICACION_EMAIL, Map.of("nombre", "Luis"), null);

        assertThat(c.html()).contains("Estudio Sonrisa").doesNotContain("vía");
    }

    @Test
    void elPieIncluyeLaLeyendaDeHerramientaDeTurnos() {
        ContenidoEmail c = plantillas.render(TipoNotificacion.RESERVA_CONFIRMADA, Map.of("nombre", "Ana"), "Consultorio Prueba");

        assertThat(c.html()).contains("herramienta de turnos");
    }

    @Test
    void elLayoutNoTieneImagenesRemotas() throws IOException {
        String layout = new String(new ClassPathResource("templates/email/_layout.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(layout).isNotBlank().doesNotContain("<img src=\"http");
    }

    @Test
    void tipoSinPlantillaFalla() {
        assertThatThrownBy(() -> plantillas.render(TipoNotificacion.OFERTA_TOMADA, Map.of(), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("OFERTA_TOMADA");
    }
}
