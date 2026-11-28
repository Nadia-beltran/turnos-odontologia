package ar.turnos.notificaciones;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ar.turnos.shared.tenancy.TenantContext;
import ar.turnos.shared.tick.TareaPeriodica;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envía las notificaciones vencidas del outbox (design D3).
 *
 * <p><b>Excepción explícita a la regla de consultas nativas</b> ({@code package-info} de {@code shared.tenancy}):
 * la selección nativa no filtra por {@code consultorio_id} porque este proceso recorre todos los consultorios
 * y corre en modo sistema. Se revisa en el PR.
 */
@Component
public class ProcesadorNotificaciones implements TareaPeriodica {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorNotificaciones.class);
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    /** Espera tras el 1.er, 2.º y 3.er fallo; el 4.º es definitivo (RN-NO-03, design D5). */
    private static final int[] ESPERAS_MINUTOS = { 5, 15, 60 };
    private static final int MAX_INTENTOS = ESPERAS_MINUTOS.length + 1;
    private static final int MAX_ERROR = 500;

    /** Clave del advisory lock que serializa las corridas (design D3). */
    static final long LOCK_ID = 7_050_001L;

    // FOR UPDATE OF n: PostgreSQL no permite bloquear el lado nulo de un LEFT JOIN.
    private static final String SELECCION = """
            SELECT n.id, c.nombre, c.email_contacto
            FROM notificacion n LEFT JOIN consultorio c ON c.id = n.consultorio_id
            WHERE n.estado = 'PENDIENTE' AND n.programada_para <= :ahora
              AND n.prioridad <= :prioridadMaxima
              AND (c.id IS NULL OR c.estado <> 'SUSPENDIDO')
            ORDER BY n.prioridad, n.programada_para
            LIMIT :limite
            FOR UPDATE OF n SKIP LOCKED
            """;

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;
    private final PlantillasEmail plantillas;
    private final EnviadorEmail enviador;
    private final JsonMapper json;
    private final Clock clock;
    private final String marca;
    private final int lote;
    private final int cuota;
    private volatile LocalDate diaDelAvisoDeCuota;

    public ProcesadorNotificaciones(PlatformTransactionManager txManager, JdbcTemplate jdbc, PlantillasEmail plantillas,
            EnviadorEmail enviador, JsonMapper json, Clock clock, @Value("${app.brand-name}") String marca,
            @Value("${app.mail.lote}") int lote,
            @Value("${app.mail.daily-quota}") int cuota) {
        this.tx = new TransactionTemplate(txManager);
        this.jdbc = jdbc;
        this.plantillas = plantillas;
        this.enviador = enviador;
        this.json = json;
        this.clock = clock;
        this.marca = marca;
        this.lote = lote;
        this.cuota = cuota;
    }

    @Override
    public void ejecutar() {
        TenantContext.comoSistema(() -> tx.executeWithoutResult(status -> procesar()));
    }

    private void procesar() {
        Boolean tomado = jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, LOCK_ID);
        if (!Boolean.TRUE.equals(tomado)) {
            return;
        }
        Instant ahora = clock.instant();
        int enviados = enviadosHoy(ahora);
        if (enviados >= cuota) {
            advertirCuotaAgotada(ahora);
            return;
        }
        // Desde el 80 % de la cuota solo salen las prioridades 1 y 2 (design D4).
        int umbral = cuota * 8 / 10;
        int prioridadMaxima = enviados >= umbral ? 2 : 4;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SELECCION)
                .setParameter("ahora", ahora).setParameter("prioridadMaxima", prioridadMaxima)
                .setParameter("limite", Math.min(lote, cuota - enviados)).getResultList();
        for (Object[] fila : filas) {
            Notificacion n = em.find(Notificacion.class, (UUID) fila[0]);
            if (enviados >= umbral && n.getPrioridad() > 2) {
                continue;   // un envío de este lote cruzó el umbral: 3 y 4 quedan sin tocar
            }
            if (enviar(n, (String) fila[1], (String) fila[2], ahora)) {
                enviados++;
            }
        }
    }

    /** Una advertencia por día (design Open Questions 4); el aviso al ADMIN_SAAS en el panel es de C-23. */
    private void advertirCuotaAgotada(Instant ahora) {
        LocalDate hoy = ahora.atZone(ZoneOffset.UTC).toLocalDate();
        if (hoy.equals(diaDelAvisoDeCuota)) {
            return;
        }
        diaDelAvisoDeCuota = hoy;
        Integer pendientes = jdbc.queryForObject(
                "select count(*) from notificacion where estado = 'PENDIENTE' and programada_para <= ?",
                Integer.class, OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
        log.warn("Cuota diaria agotada ({} emails): {} notificaciones pendientes hasta mañana", cuota, pendientes);
    }

    /** Enviados desde las 00:00 UTC del día de {@code ahora} (design D5). */
    private int enviadosHoy(Instant ahora) {
        Instant inicioDia = ahora.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.queryForObject("select count(*) from notificacion where estado = 'ENVIADA' and enviada_at >= ?",
                Integer.class, OffsetDateTime.ofInstant(inicioDia, ZoneOffset.UTC));
    }

    /** @return si el email salió; un fallo no corta el lote (design D5) */
    private boolean enviar(Notificacion n, String consultorio, String emailContacto, Instant ahora) {
        try {
            Map<String, Object> payload = json.readValue(n.getPayload(), MAPA);
            ContenidoEmail c = plantillas.render(n.getTipo(), payload, consultorio);
            String remitente = consultorio != null ? consultorio + " vía " + marca : marca;
            enviador.enviar(new EmailSaliente(n.getDestinatario(), remitente, emailContacto, n.getTipo().asunto(),
                    c.html(), c.texto()));
        } catch (RuntimeException e) {
            registrarFallo(n, e, ahora);
            return false;
        }
        n.marcarEnviada(ahora);
        log.info("Notificación enviada: id={}", n.getId());
        return true;
    }

    private void registrarFallo(Notificacion n, RuntimeException e, Instant ahora) {
        int intentos = n.getIntentos() + 1;
        Instant reintento = intentos < MAX_INTENTOS ? ahora.plus(ESPERAS_MINUTOS[intentos - 1], ChronoUnit.MINUTES) : null;
        // El texto del error puede traer el destinatario: va solo a la base, nunca al log (RN-SG-06).
        String error = e.getClass().getName() + ": " + e.getMessage();
        n.registrarFallo(error.length() > MAX_ERROR ? error.substring(0, MAX_ERROR) : error, reintento);
        log.warn("Envío fallido: id={} intentos={} causa={}", n.getId(), intentos, e.getClass().getSimpleName());
    }
}
