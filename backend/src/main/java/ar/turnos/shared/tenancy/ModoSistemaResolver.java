package ar.turnos.shared.tenancy;

import java.util.function.Supplier;

/** Parámetro {@code modoSistema} del filtro. */
public class ModoSistemaResolver implements Supplier<Boolean> {

    @Override
    public Boolean get() {
        return TenantContext.esModoSistema();
    }
}
