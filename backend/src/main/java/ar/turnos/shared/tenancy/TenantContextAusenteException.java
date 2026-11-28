package ar.turnos.shared.tenancy;

/** No hay consultorio en el contexto y tampoco modo sistema: la operación falla cerrada. */
public class TenantContextAusenteException extends RuntimeException {

    public TenantContextAusenteException() {
        super("No hay consultorio en el contexto de la operación");
    }
}
