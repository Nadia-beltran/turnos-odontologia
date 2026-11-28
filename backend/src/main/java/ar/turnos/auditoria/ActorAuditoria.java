package ar.turnos.auditoria;

import java.util.UUID;

/** Actor de una acción auditada: tipo y, si es {@code USUARIO}, su id. */
public record ActorAuditoria(ActorTipo tipo, UUID usuarioId) {

    public static final ActorAuditoria SISTEMA = new ActorAuditoria(ActorTipo.SISTEMA, null);
}
