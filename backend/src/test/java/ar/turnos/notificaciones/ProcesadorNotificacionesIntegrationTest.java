package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import ar.turnos.shared.MailpitClient;
import ar.turnos.shared.tenancy.AbstractTenantIsolationTest;
import ar.turnos.shared.tenancy.TenantContext;
import tools.jackson.databind.JsonNode;

/**
 * Spec email-notifications: envío, prioridad, cuota, reintentos, suspendidos, concurrencia y logs.
 * La cuota de 10 emails hace baratos los escenarios de cuota (design D8). Cada caso parte de un outbox vacío,
 * porque el procesador recorre todos los consultorios.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "app.mail.daily-quota=10")
class ProcesadorNotificacionesIntegrationTest extends AbstractTenantIsolationTest {

    @Autowired
    ProcesadorNotificaciones procesador;
    @Autowired
    NotificacionService service;
    @Autowired
    MailpitClient mailpit;
    @MockitoSpyBean
    EnviadorEmail enviador;

    @BeforeEach
    void outboxVacio() {
        jdbc.update("delete from notificacion");
        mailpit.vaciar();
    }

    /** Encola una notificación del consultorio A y devuelve su destinatario (único por llamada). */
    private String encolar(TipoNotificacion tipo, Instant programadaPara) {
        String dest = "paciente-" + UUID.randomUUID() + "@example.test";
        comoConsultorio(consultorioA(), () -> service.encolar(tipo, dest, Map.of("nombre", "Ana"), programadaPara));
        return dest;
    }

    private Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }

    private Map<String, Object> fila(String dest) {
        return jdbc.queryForMap("select estado, intentos, enviada_at, programada_para, ultimo_error from notificacion where destinatario = ?", dest);
    }

    private List<String> destinatariosEnMailpit() {
        return mailpit.mensajes().stream().map((JsonNode m) -> m.get("To").get(0).get("Address").asString()).toList();
    }

    @Test
    void envioExitosoMarcaEnviadaYMailpitRecibeElEmail() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(dest);
        Map<String, Object> f = fila(dest);
        assertThat(f).containsEntry("estado", "ENVIADA");
        assertThat(((Timestamp) f.get("enviada_at")).toInstant()).isBetween(Instant.now().minusSeconds(30), Instant.now().plusSeconds(1));
    }

    @Test
    void variasNotificacionesVencidasSeEnvianTodas() {
        String d1 = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        String d2 = encolar(TipoNotificacion.RECORDATORIO, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactlyInAnyOrder(d1, d2);
    }

    @Test
    void notificacionFuturaNoSeEnviaYSigueEnPendiente() {
        String dest = encolar(TipoNotificacion.RECORDATORIO, Instant.now().plus(1, ChronoUnit.HOURS));

        procesador.ejecutar();

        assertThat(mailpit.mensajes()).isEmpty();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE");
    }

    // --- Prioridad y cuota (design D4)

    /** Inserta {@code n} envíos ya hechos con la fecha dada, para consumir cuota. */
    private void enviadosEl(int n, Instant enviadaAt) {
        for (int i = 0; i < n; i++) {
            jdbc.update("insert into notificacion (id, tipo, prioridad, destinatario, payload, estado, intentos, programada_para, enviada_at) "
                    + "values (?, 'RESERVA_CONFIRMADA', 2, 'viejo@example.test', '{}'::jsonb, 'ENVIADA', 1, ?, ?)",
                    UUID.randomUUID(), OffsetDateTime.ofInstant(enviadaAt, ZoneOffset.UTC), OffsetDateTime.ofInstant(enviadaAt, ZoneOffset.UTC));
        }
    }

    private Instant inicioDeHoyUtc() {
        return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Test
    void prioridadAntesQueAntiguedad() {
        enviadosEl(9, inicioDeHoyUtc().plusSeconds(1));
        String p4 = encolar(TipoNotificacion.INSCRIPCION_ESPERA, Instant.now().minus(1, ChronoUnit.HOURS));
        String p1 = encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(p1);
        assertThat(fila(p1)).containsEntry("estado", "ENVIADA");
        assertThat(fila(p4)).containsEntry("estado", "PENDIENTE");
    }

    @Test
    void aIgualPrioridadVaPrimeroLaMasAntigua() {
        enviadosEl(9, inicioDeHoyUtc().plusSeconds(1));
        String nueva = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        String vieja = encolar(TipoNotificacion.RESERVA_CONFIRMADA, Instant.now().minus(1, ChronoUnit.HOURS));

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(vieja);
        assertThat(fila(nueva)).containsEntry("estado", "PENDIENTE");
    }

    @Test
    void cuotaAgotadaNoEnviaNadaYNoSumaIntentos() {
        enviadosEl(10, inicioDeHoyUtc().plusSeconds(1));
        String dest = encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());

        procesador.ejecutar();

        assertThat(mailpit.mensajes()).isEmpty();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0);
    }

    @Test
    void desdeElUmbralDel80PorCientoSoloPrioridades1y2() {
        enviadosEl(8, inicioDeHoyUtc().plusSeconds(1));
        String p2 = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        String p3 = encolar(TipoNotificacion.RECORDATORIO, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(p2);
        assertThat(fila(p3)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0);
    }

    @Test
    void enElUmbralLaPrioridad1TambienSeEnvia() {
        enviadosEl(8, inicioDeHoyUtc().plusSeconds(1));
        String p1 = encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(p1);
    }

    @Test
    void porDebajoDelUmbralSeEnvianTodasLasPrioridades() {
        enviadosEl(7, inicioDeHoyUtc().plusSeconds(1));
        String p4 = encolar(TipoNotificacion.INSCRIPCION_ESPERA, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(p4);
    }

    @Test
    void laCuotaSeAgotaDentroDelLote() {
        enviadosEl(9, inicioDeHoyUtc().plusSeconds(1));
        encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());
        encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());

        procesador.ejecutar();

        assertThat(mailpit.mensajes()).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from notificacion where estado = 'PENDIENTE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void siUnEnvioCruzaElUmbralDentroDelLoteLasPrioridades3y4NoSeTocan() {
        enviadosEl(7, inicioDeHoyUtc().plusSeconds(1));
        String p2 = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        String p3 = encolar(TipoNotificacion.RECORDATORIO, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(p2);
        assertThat(fila(p3)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0);
    }

    @Test
    void enUnDiaNuevoLaCuotaVuelveAEstarDisponible() {
        enviadosEl(10, inicioDeHoyUtc().minusSeconds(3600));
        String dest = encolar(TipoNotificacion.RECORDATORIO, haceUnMinuto());

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(dest);
    }

    // --- Reintentos (design D5)

    private void elProveedorRechaza() {
        doThrow(new MailSendException("rechazado")).when(enviador).enviar(any());
    }

    private void conIntentos(String dest, int intentos) {
        jdbc.update("update notificacion set intentos = ? where destinatario = ?", intentos, dest);
    }

    private void assertReprogramada(String dest, Instant antes, long minutos) {
        Instant programada = ((Timestamp) fila(dest).get("programada_para")).toInstant();
        // Tolerancia: la corrida ocurre entre "antes" y "ahora".
        assertThat(programada).isBetween(antes.plus(minutos, ChronoUnit.MINUTES), Instant.now().plus(minutos, ChronoUnit.MINUTES));
    }

    @Test
    void primerFalloReprogramaA5MinutosYGuardaElError() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        elProveedorRechaza();
        Instant antes = Instant.now();

        procesador.ejecutar();

        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 1);
        assertThat((String) fila(dest).get("ultimo_error")).contains("MailSendException");
        assertReprogramada(dest, antes, 5);
    }

    @Test
    void segundoYTercerFalloEsperan15Y60Minutos() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        elProveedorRechaza();

        conIntentos(dest, 1);
        Instant antes = Instant.now();
        procesador.ejecutar();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 2);
        assertReprogramada(dest, antes, 15);

        jdbc.update("update notificacion set programada_para = ?, intentos = 2 where destinatario = ?",
                OffsetDateTime.ofInstant(haceUnMinuto(), ZoneOffset.UTC), dest);
        antes = Instant.now();
        procesador.ejecutar();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 3);
        assertReprogramada(dest, antes, 60);
    }

    @Test
    void reintentoExitosoQuedaEnviada() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        conIntentos(dest, 1);

        procesador.ejecutar();

        assertThat(fila(dest)).containsEntry("estado", "ENVIADA");
        assertThat(destinatariosEnMailpit()).containsExactly(dest);
    }

    @Test
    void falloDefinitivoQuedaFallidaYNoSeReenvia() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        conIntentos(dest, 3);
        elProveedorRechaza();

        procesador.ejecutar();

        assertThat(fila(dest)).containsEntry("estado", "FALLIDA").containsEntry("intentos", 4);

        org.mockito.Mockito.reset(enviador);
        procesador.ejecutar();
        assertThat(mailpit.mensajes()).isEmpty();
        assertThat(fila(dest)).containsEntry("estado", "FALLIDA").containsEntry("intentos", 4);
    }

    @Test
    void proveedorCaidoNoPierdeNingunaNotificacion() {
        String d1 = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        String d2 = encolar(TipoNotificacion.RECORDATORIO, haceUnMinuto());
        String d3 = encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());
        elProveedorRechaza();

        procesador.ejecutar();

        for (String d : List.of(d1, d2, d3)) {
            assertThat(fila(d)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 1);
        }
    }

    // --- Consultorios suspendidos y del sistema

    private String encolarDelSistema() {
        String dest = "admin-" + UUID.randomUUID() + "@example.test";
        TenantContext.comoSistema(() -> tx.execute(status ->
                service.encolar(TipoNotificacion.VERIFICACION_EMAIL, dest, Map.of("nombre", "Admin"), haceUnMinuto())));
        return dest;
    }

    private void estadoDelConsultorioA(String estado) {
        jdbc.update("update consultorio set estado = ? where id = ?", estado, consultorioA());
    }

    @Test
    void consultorioSuspendidoNoEnviaYNoSumaIntentos() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        estadoDelConsultorioA("SUSPENDIDO");

        procesador.ejecutar();

        assertThat(mailpit.mensajes()).isEmpty();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0);
    }

    @Test
    void consultorioPendienteDeVerificacionSiEnvia() {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        estadoDelConsultorioA("PENDIENTE_VERIFICACION");

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(dest);
    }

    @Test
    void laNotificacionSinConsultorioSeEnviaSoloConLaMarcaYSinReplyTo() {
        String dest = encolarDelSistema();

        procesador.ejecutar();

        JsonNode m = mailpit.mensajes().get(0);
        assertThat(m.get("To").get(0).get("Address").asString()).isEqualTo(dest);
        assertThat(m.get("From").get("Name").asString()).isEqualTo("Estudio Sonrisa");
        assertThat(m.get("ReplyTo")).isNullOrEmpty();
    }

    @Test
    void elEmailDeUnConsultorioSaleConSuNombreYSuReplyTo() {
        encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());

        procesador.ejecutar();

        JsonNode m = mailpit.mensajes().get(0);
        assertThat(m.get("From").get("Name").asString()).isEqualTo("Consultorio de prueba vía Estudio Sonrisa");
        assertThat(m.get("ReplyTo").get(0).get("Address").asString()).isEqualTo("contacto@ejemplo.test");
    }

    // --- Concurrencia (design D3)

    @Test
    void dosProcesamientosSimultaneosEnvianUnSoloEmail() throws Exception {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<?>> corridas = List.of(
                    pool.submit(() -> {
                        largada.await();
                        procesador.ejecutar();
                        return null;
                    }),
                    pool.submit(() -> {
                        largada.await();
                        procesador.ejecutar();
                        return null;
                    }));
            largada.countDown();
            for (Future<?> f : corridas) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(destinatariosEnMailpit()).containsExactly(dest);
        assertThat(fila(dest)).containsEntry("estado", "ENVIADA");
    }

    @Test
    void conElLockTomadoOtraCorridaNoHaceNadaYDespuesSiEnvia() throws Exception {
        String dest = encolar(TipoNotificacion.RESERVA_CONFIRMADA, haceUnMinuto());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            tx.executeWithoutResult(status -> {
                jdbc.queryForObject("select pg_advisory_xact_lock(?) is not null", Boolean.class, ProcesadorNotificaciones.LOCK_ID);
                try {
                    pool.submit(procesador::ejecutar).get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } finally {
            pool.shutdownNow();
        }
        assertThat(mailpit.mensajes()).isEmpty();
        assertThat(fila(dest)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0);

        procesador.ejecutar();

        assertThat(destinatariosEnMailpit()).containsExactly(dest);
    }

    // --- Logs sin datos personales (RN-SG-06)

    @Test
    void elEnvioFallidoSeRegistraConIdYSinDestinatarioNiContenidoNiMensajeDelError(CapturedOutput salida) {
        String dest = "paciente-log-" + UUID.randomUUID() + "@example.test";
        comoConsultorio(consultorioA(), () -> service.encolar(TipoNotificacion.RESERVA_CONFIRMADA, dest,
                Map.of("nombre", "NombreSecretoFicticio"), haceUnMinuto()));
        doThrow(new MailSendException("el proveedor rechazó " + dest + " NombreSecretoFicticio")).when(enviador).enviar(any());

        procesador.ejecutar();

        UUID id = jdbc.queryForObject("select id from notificacion where destinatario = ?", UUID.class, dest);
        assertThat(salida.getAll()).contains(id.toString()).doesNotContain(dest).doesNotContain("NombreSecretoFicticio")
                .doesNotContain("el proveedor rechazó");
    }

    @Test
    void laCuotaAgotadaAdviertePorLogUnaVezPorDia(CapturedOutput salida) {
        ReflectionTestUtils.setField(procesador, "diaDelAvisoDeCuota", null);
        enviadosEl(10, inicioDeHoyUtc().plusSeconds(1));
        encolar(TipoNotificacion.VERIFICACION_EMAIL, haceUnMinuto());

        procesador.ejecutar();
        procesador.ejecutar();

        assertThat(salida.getAll().split("Cuota diaria agotada", -1).length - 1).isEqualTo(1);
        assertThat(salida.getAll()).contains("1 notificaciones pendientes");
    }
}
