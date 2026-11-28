package ar.turnos.auditoria;

/**
 * Resuelve el actor de la acción en curso (design D5). C-04 aporta el bean por defecto ({@code SISTEMA});
 * C-06 define el suyo desde {@code auth} y reemplaza al por defecto sin editar este módulo.
 */
public interface ActorAuditoriaProvider {

    ActorAuditoria actual();
}
