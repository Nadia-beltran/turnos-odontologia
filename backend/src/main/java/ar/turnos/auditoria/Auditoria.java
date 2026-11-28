package ar.turnos.auditoria;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila append-only (RN-CL-01). No extiende BaseTenantEntity: su consultorio es nullable (design D5).
 * La base rechaza UPDATE y DELETE con un trigger.
 */
@Entity
@Immutable
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultorio_id")
    private UUID consultorioId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_tipo", nullable = false)
    private ActorTipo actorTipo;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 40)
    private String entidad;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String datos;

    @JdbcTypeCode(SqlTypes.INET)
    private InetAddress ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Auditoria() {
    }

    Auditoria(UUID consultorioId, ActorAuditoria actor, String accion, String entidad, UUID entidadId,
            String datos, InetAddress ip) {
        this.consultorioId = consultorioId;
        this.actorTipo = actor.tipo();
        this.usuarioId = actor.usuarioId();
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.datos = datos;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public UUID getConsultorioId() {
        return consultorioId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public ActorTipo getActorTipo() {
        return actorTipo;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public UUID getEntidadId() {
        return entidadId;
    }

    public String getDatos() {
        return datos;
    }

    public InetAddress getIp() {
        return ip;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
