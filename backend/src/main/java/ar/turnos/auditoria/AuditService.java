package ar.turnos.auditoria;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ar.turnos.shared.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Registra acciones auditadas dentro de la transacción de quien llama (design D5). */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_ACCION = 60;

    @PersistenceContext
    private EntityManager em;

    private final ActorAuditoriaProvider actores;
    private final JsonMapper json;

    public AuditService(ActorAuditoriaProvider actores, JsonMapper json) {
        this.actores = actores;
        this.json = json;
    }

    /**
     * @param datos detalle o estado previo (se guarda como JSON; nunca se escribe en el log, RN-SG-06)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Auditoria registrar(String accion, String entidad, UUID entidadId, Object datos) {
        if (accion == null || accion.isBlank() || accion.length() > MAX_ACCION) {
            throw new IllegalArgumentException("La acción es obligatoria y de hasta " + MAX_ACCION + " caracteres");
        }
        Auditoria fila = new Auditoria(TenantContext.actual().orElse(null), actores.actual(), accion, entidad,
                entidadId, serializar(datos), ipDeLaRequest());
        em.persist(fila);
        log.info("Auditoría registrada: accion={} entidadId={}", accion, entidadId);
        return fila;
    }

    private String serializar(Object datos) {
        if (datos == null) {
            return null;
        }
        try {
            return datos instanceof String s ? s : json.writeValueAsString(datos);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("El detalle de auditoría no se puede serializar", e);
        }
    }

    private InetAddress ipDeLaRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            try {
                return InetAddress.getByName(attrs.getRequest().getRemoteAddr());
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }
}
