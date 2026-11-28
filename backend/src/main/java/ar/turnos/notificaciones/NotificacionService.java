package ar.turnos.notificaciones;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encola emails en el outbox dentro de la transacción de quien llama (RN-NO-01). Ver {@code package-info}.
 */
@Service
public class NotificacionService {

    private static final int MAX_DESTINATARIO = 254;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final NotificacionRepository repo;
    private final PlantillasEmail plantillas;
    private final JsonMapper json;
    private final Clock clock;

    public NotificacionService(NotificacionRepository repo, PlantillasEmail plantillas, JsonMapper json, Clock clock) {
        this.repo = repo;
        this.plantillas = plantillas;
        this.json = json;
        this.clock = clock;
    }

    /**
     * El consultorio sale del contexto de tenancy, nunca de un parámetro (RN-MT-02).
     *
     * @param programadaPara cuándo enviar; {@code null} = ahora
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Notificacion encolar(TipoNotificacion tipo, String destinatario, Map<String, Object> payload,
            Instant programadaPara) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        if (payload == null) {
            throw new IllegalArgumentException("El contenido de la notificación es obligatorio");
        }
        if (destinatario == null || destinatario.length() > MAX_DESTINATARIO || !EMAIL.matcher(destinatario).matches()) {
            throw new IllegalArgumentException("El destinatario no es un email válido");
        }
        if (!plantillas.existen(tipo)) {
            throw new IllegalArgumentException("El tipo " + tipo + " no tiene plantillas HTML y de texto plano");
        }
        Notificacion n = new Notificacion(tipo, destinatario, serializar(payload),
                programadaPara != null ? programadaPara : clock.instant());
        return repo.save(n);
    }

    private String serializar(Map<String, Object> payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("El contenido de la notificación no se puede serializar", e);
        }
    }
}
