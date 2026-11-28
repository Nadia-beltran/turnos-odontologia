package ar.turnos.shared.tick;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.turnos.notificaciones.NotificacionService;
import ar.turnos.notificaciones.TipoNotificacion;
import ar.turnos.shared.MailpitClient;
import ar.turnos.shared.tenancy.AbstractTenantIsolationTest;

/** Spec periodic-tasks: el tick ejecuta el outbox y es idempotente. */
class TickIntegrationTest extends AbstractTenantIsolationTest {

    @Autowired
    TickRunner tick;
    @Autowired
    NotificacionService service;
    @Autowired
    MailpitClient mailpit;

    private String dest;

    @BeforeEach
    void outboxConUnaNotificacionVencida() {
        jdbc.update("delete from notificacion");
        mailpit.vaciar();
        dest = "paciente-" + UUID.randomUUID() + "@example.test";
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest,
                Map.of("nombre", "Ana"), Instant.now().minus(1, ChronoUnit.MINUTES)));
    }

    private String estado() {
        return jdbc.queryForObject("select estado from notificacion where destinatario = ?", String.class, dest);
    }

    @Test
    void elTickProcesaElOutbox() {
        tick.ejecutar();

        assertThat(estado()).isEqualTo("ENVIADA");
        assertThat(mailpit.mensajes()).hasSize(1);
    }

    @Test
    void elTickRepetidoNoEnviaDeNuevo() {
        tick.ejecutar();
        tick.ejecutar();

        assertThat(mailpit.mensajes()).hasSize(1);
        assertThat(estado()).isEqualTo("ENVIADA");
    }
}
