package ar.turnos.shared.tenancy;

import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Base de toda entidad de negocio con {@code consultorio_id} (design D1 y D3). Ver {@code package-info}.
 * Las lecturas se filtran por consultorio y las escrituras se validan contra el contexto.
 */
@MappedSuperclass
@FilterDef(name = "tenant",
        parameters = {
                @ParamDef(name = "consultorioId", type = UUID.class, resolver = ConsultorioActualResolver.class),
                @ParamDef(name = "modoSistema", type = Boolean.class, resolver = ModoSistemaResolver.class) },
        defaultCondition = "(:modoSistema = true or consultorio_id = :consultorioId)",
        autoEnabled = true,
        applyToLoadByKey = true)
@Filter(name = "tenant")
public abstract class BaseTenantEntity {

    @Column(name = "consultorio_id", nullable = false, updatable = false)
    private UUID consultorioId;

    public UUID getConsultorioId() {
        return consultorioId;
    }

    @PrePersist
    void asignarConsultorio() {
        if (consultorioId == null) {
            // En modo sistema no hay consultorio que asignar: la entidad ya debe traerlo.
            consultorioId = TenantContext.requerido();
        } else if (!TenantContext.esModoSistema() && !consultorioId.equals(TenantContext.requerido())) {
            throw new TenantViolationException("No se puede crear un dato de otro consultorio");
        }
    }

    @PreUpdate
    @PreRemove
    void validarConsultorio() {
        if (!TenantContext.esModoSistema() && !consultorioId.equals(TenantContext.requerido())) {
            throw new TenantViolationException("No se puede modificar un dato de otro consultorio");
        }
    }
}
