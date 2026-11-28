package ar.turnos.shared.tenancy;

import java.util.UUID;
import java.util.function.Supplier;

/** Parámetro {@code consultorioId} del filtro: falla cerrado si no hay consultorio ni modo sistema. */
public class ConsultorioActualResolver implements Supplier<UUID> {

    /** UUID nulo: valor que no coincide con ningún consultorio en modo sistema (no se usa null). */
    static final UUID NINGUNO = new UUID(0L, 0L);

    @Override
    public UUID get() {
        if (TenantContext.esModoSistema()) {
            return NINGUNO;
        }
        return TenantContext.requerido();
    }
}
