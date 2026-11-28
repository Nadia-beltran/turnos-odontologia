package ar.turnos.auditoria;

/** Quién originó la acción auditada. Solo {@code USUARIO} lleva usuario. */
public enum ActorTipo {
    USUARIO, PACIENTE, SISTEMA
}
