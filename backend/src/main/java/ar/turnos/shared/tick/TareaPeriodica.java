package ar.turnos.shared.tick;

/**
 * Trabajo que corre en cada tick (cada 5 minutos o cuando lo invoca el tick externo). Debe ser idempotente:
 * repetir el tick no puede repetir trabajo ya hecho. El orden de ejecución sale de {@code @Order}.
 */
public interface TareaPeriodica {

    void ejecutar();

    /** Nombre para el log; nunca debe incluir datos personales. */
    default String nombre() {
        return getClass().getSimpleName();
    }
}
