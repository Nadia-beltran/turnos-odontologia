package ar.turnos.notificaciones;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import ar.turnos.shared.tenancy.TenantContext;
import ar.turnos.shared.tenancy.TenantViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Fila del outbox (design D1). Es tenant pero con {@code consultorio_id} nullable (emails del sistema), así que
 * no extiende {@code BaseTenantEntity}: declara el filtro y sus propias guardas, equivalentes a las de la base.
 */
@Entity
@Table(name = "notificacion")
@Filter(name = "tenant")
public class Notificacion {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "consultorio_id", updatable = false)
    private UUID consultorioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoNotificacion tipo;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false, updatable = false)
    private int prioridad;

    @Column(nullable = false, updatable = false)
    private String destinatario;

    /** JSON con las variables de la plantilla. Nunca DNI ni datos clínicos (RN-SG-06). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int intentos;

    @Column(name = "programada_para", nullable = false)
    private Instant programadaPara;

    @Column(name = "enviada_at")
    private Instant enviadaAt;

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    protected Notificacion() {
    }

    Notificacion(TipoNotificacion tipo, String destinatario, String payload, Instant programadaPara) {
        this.tipo = tipo;
        this.prioridad = tipo.prioridad();
        this.destinatario = destinatario;
        this.payload = payload;
        this.programadaPara = programadaPara;
    }

    void marcarEnviada(Instant ahora) {
        this.estado = EstadoNotificacion.ENVIADA;
        this.enviadaAt = ahora;
        this.ultimoError = null;
    }

    /** Suma un intento; con reintento reprograma, sin él queda FALLIDA (design D5). */
    void registrarFallo(String error, Instant reprogramarPara) {
        this.intentos++;
        this.ultimoError = error;
        if (reprogramarPara == null) {
            this.estado = EstadoNotificacion.FALLIDA;
        } else {
            this.programadaPara = reprogramarPara;
        }
    }

    @PrePersist
    void asignarConsultorio() {
        if (consultorioId == null) {
            // En modo sistema se permite quedar sin consultorio; fuera de él, falla cerrado.
            if (!TenantContext.esModoSistema()) {
                consultorioId = TenantContext.requerido();
            }
        } else if (!TenantContext.esModoSistema() && !consultorioId.equals(TenantContext.requerido())) {
            throw new TenantViolationException("No se puede crear un dato de otro consultorio");
        }
    }

    @PreUpdate
    @PreRemove
    void validarConsultorio() {
        if (!TenantContext.esModoSistema() && !TenantContext.requerido().equals(consultorioId)) {
            throw new TenantViolationException("No se puede modificar un dato de otro consultorio");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsultorioId() {
        return consultorioId;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getPayload() {
        return payload;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getProgramadaPara() {
        return programadaPara;
    }

    public Instant getEnviadaAt() {
        return enviadaAt;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
