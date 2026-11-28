package ar.turnos.shared.tenancy;

/** Una escritura intentó tocar datos de otro consultorio (RN-MT-01, RN-MT-02). */
public class TenantViolationException extends RuntimeException {

    public TenantViolationException(String mensaje) {
        super(mensaje);
    }
}
