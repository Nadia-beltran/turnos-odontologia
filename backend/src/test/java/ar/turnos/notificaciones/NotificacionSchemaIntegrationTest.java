package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.turnos.shared.AbstractIntegrationTest;

/** V4: la base rechaza por sí sola los estados inválidos del outbox (design D1). */
class NotificacionSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private void insertar(String tipo, int prioridad, String estado, int intentos, String enviadaAt) {
        jdbc.update("insert into notificacion (id, tipo, prioridad, destinatario, payload, estado, intentos, programada_para, enviada_at) "
                + "values (?, ?, ?, 'p@example.test', '{}'::jsonb, ?, ?, now(), " + enviadaAt + ")",
                UUID.randomUUID(), tipo, prioridad, estado, intentos);
    }

    @Test
    void v4QuedaAplicadaYUnaFilaValidaSeInserta() {
        Integer aplicada = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '4' and success", Integer.class);
        assertThat(aplicada).isEqualTo(1);
        insertar("RESERVA_CONFIRMADA", 2, "PENDIENTE", 0, "null");
        insertar("RESERVA_CONFIRMADA", 2, "ENVIADA", 1, "now()");
    }

    @Test
    void rechazaTipoDesconocido() {
        assertThatThrownBy(() -> insertar("TIPO_INEXISTENTE", 2, "PENDIENTE", 0, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaPrioridadFueraDeRango() {
        assertThatThrownBy(() -> insertar("RECORDATORIO", 0, "PENDIENTE", 0, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("RECORDATORIO", 5, "PENDIENTE", 0, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaEstadoDesconocidoEIntentosNegativos() {
        assertThatThrownBy(() -> insertar("RECORDATORIO", 3, "EN_PROCESO", 0, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("RECORDATORIO", 3, "PENDIENTE", -1, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enviadaExigeFechaDeEnvioYSoloEnviadaLaTiene() {
        assertThatThrownBy(() -> insertar("RECORDATORIO", 3, "ENVIADA", 1, "null"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("RECORDATORIO", 3, "PENDIENTE", 0, "now()"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
