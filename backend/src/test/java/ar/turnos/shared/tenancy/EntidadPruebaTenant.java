package ar.turnos.shared.tenancy;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Entidad solo de test (design D6): ejercita BaseTenantEntity sobre la tabla prueba_tenant. */
@Entity
@Table(name = "prueba_tenant")
public class EntidadPruebaTenant extends BaseTenantEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String valor;

    protected EntidadPruebaTenant() {
    }

    public EntidadPruebaTenant(String valor) {
        this.valor = valor;
    }


    public UUID getId() {
        return id;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
