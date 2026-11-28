package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.turnos.shared.MailpitClient;
import ar.turnos.shared.tenancy.AbstractTenantIsolationTest;
import ar.turnos.shared.tenancy.TenantContext;
import ar.turnos.shared.tenancy.TenantContextAusenteException;

/** Spec email-notifications: encolado transaccional, datos válidos y aislamiento por consultorio. */
class NotificacionServiceIntegrationTest extends AbstractTenantIsolationTest {

    @Autowired
    NotificacionService service;
    @Autowired
    NotificacionRepository repo;
    @Autowired
    MailpitClient mailpit;

    private Map<String, Object> payload() {
        return Map.of("nombre", "Ana");
    }

    private String destinatario() {
        return "paciente-" + UUID.randomUUID() + "@example.test";
    }

    private int contar(String destinatario) {
        return jdbc.queryForObject("select count(*) from notificacion where destinatario = ?", Integer.class, destinatario);
    }

    private Instant programadaPara(String destinatario) {
        return jdbc.queryForObject("select programada_para from notificacion where destinatario = ?",
                Timestamp.class, destinatario).toInstant();
    }

    // --- Encolado transaccional

    @Test
    void eventoConfirmadoDejaUnaNotificacionPendienteSinEnviarNada() {
        String dest = destinatario();
        mailpit.vaciar();
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest, payload(), null));

        Map<String, Object> fila = jdbc.queryForMap(
                "select consultorio_id, estado, intentos, prioridad, tipo from notificacion where destinatario = ?", dest);
        assertThat(fila).containsEntry("consultorio_id", consultorioA()).containsEntry("estado", "PENDIENTE")
                .containsEntry("tipo", "RESERVA_CONFIRMADA");
        assertThat(((Number) fila.get("intentos")).intValue()).isZero();
        assertThat(((Number) fila.get("prioridad")).intValue()).isEqualTo(2);
        assertThat(mailpit.mensajes()).isEmpty();
    }

    @Test
    void laPrioridadSeCopiaDelTipo() {
        String dest = destinatario();
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RECORDATORIO, dest, payload(), null));
        assertThat(jdbc.queryForObject("select prioridad from notificacion where destinatario = ?", Integer.class, dest)).isEqualTo(3);
    }

    @Test
    void eventoRevertidoNoDejaNotificacion() {
        String dest = destinatario();
        assertThatThrownBy(() -> comoConsultorio(consultorioA(), () -> {
            service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest, payload(), null);
            throw new IllegalStateException("falla el evento");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(contar(dest)).isZero();
    }

    @Test
    void sinFechaProgramadaQuedaParaElMomentoDelEncolado() {
        String dest = destinatario();
        Instant antes = Instant.now().minusSeconds(1);
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest, payload(), null));
        assertThat(programadaPara(dest)).isBetween(antes, Instant.now().plusSeconds(1));
    }

    @Test
    void conFechaProgramadaLaRespeta() {
        String dest = destinatario();
        Instant futura = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RECORDATORIO, dest, payload(), futura));
        assertThat(programadaPara(dest)).isEqualTo(futura);
    }

    // --- Datos válidos

    @Test
    void destinatarioInvalidoSeRechazaSinGuardarNada() {
        for (String malo : List.of("no-es-un-email", "a@b", " ", "x".repeat(250) + "@example.test")) {
            assertThatThrownBy(() -> comoConsultorio(consultorioA(),
                    () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, malo, payload(), null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(contar("no-es-un-email")).isZero();
    }

    @Test
    void faltanTipoOContenidoSeRechaza() {
        assertThatThrownBy(() -> comoConsultorio(consultorioA(), () -> service.encolar(null, destinatario(), payload(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> comoConsultorio(consultorioA(),
                () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, destinatario(), null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tipoSinPlantillasSeRechazaNombrandoElTipo() {
        String dest = destinatario();
        assertThatThrownBy(() -> comoConsultorio(consultorioA(),
                () -> service.encolar(TipoNotificacion.OFERTA_TOMADA, dest, payload(), null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("OFERTA_TOMADA");
        assertThat(contar(dest)).isZero();
    }

    // --- Aislamiento

    @Test
    void listadoPropio() {
        String da = destinatario();
        String db = destinatario();
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, da, payload(), null));
        comoConsultorio(consultorioB(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, db, payload(), null));

        List<Notificacion> vistasPorA = comoConsultorio(consultorioA(), repo::findAll);
        assertThat(vistasPorA).extracting(Notificacion::getDestinatario).contains(da).doesNotContain(db);
        assertThat(vistasPorA).allMatch(n -> consultorioA().equals(n.getConsultorioId()));
    }

    @Test
    void buscarPorIdAjenoDevuelveVacio() {
        String db = destinatario();
        comoConsultorio(consultorioB(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, db, payload(), null));
        UUID idB = jdbc.queryForObject("select id from notificacion where destinatario = ?", UUID.class, db);
        assertThat(comoConsultorio(consultorioA(), () -> repo.findById(idB))).isEmpty();
        assertThat(comoConsultorio(consultorioB(), () -> repo.findById(idB))).isPresent();
    }

    @Test
    void encolarSinContextoSeRechazaYNoGuardaNada() {
        String dest = destinatario();
        assertThatThrownBy(() -> tx.execute(status ->
                service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest, payload(), null)))
                .isInstanceOf(TenantContextAusenteException.class);
        assertThat(contar(dest)).isZero();
    }

    @Test
    void enModoSistemaQuedaSinConsultorioYNingunConsultorioLaVe() {
        String dest = destinatario();
        TenantContext.comoSistema(() -> tx.execute(status ->
                service.encolar(TipoNotificacion.RECORDATORIO, dest, payload(), null)));
        assertThat(jdbc.queryForObject("select consultorio_id from notificacion where destinatario = ?", UUID.class, dest)).isNull();
        List<Notificacion> vistas = comoConsultorio(consultorioA(), repo::findAll);
        assertThat(vistas).extracting(Notificacion::getDestinatario).doesNotContain(dest);
    }

    @Test
    void elConsultorioNoSeAceptaDeAfuera() {
        // encolar no tiene parámetro de consultorio (RN-MT-02): siempre toma el del contexto.
        assertThat(NotificacionService.class.getDeclaredMethods())
                .filteredOn(m -> m.getName().equals("encolar"))
                .isNotEmpty()
                .allSatisfy(m -> assertThat(List.of((Object[]) m.getParameterTypes())).doesNotContain(UUID.class));
    }
}
