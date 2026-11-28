package ar.turnos.shared.tick;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecución programada del tick (spec periodic-tasks). Hace exactamente lo mismo que el tick externo:
 * llama a {@link TickRunner}. Se desactiva con {@code app.tick.programado=false} (los tests).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.tick.programado", havingValue = "true", matchIfMissing = true)
public class TickProgramado {

    private final TickRunner tick;

    public TickProgramado(TickRunner tick) {
        this.tick = tick;
    }

    // initialDelay = intervalo: no dispara durante el arranque, antes de que la app esté lista.
    @Scheduled(fixedDelayString = "${app.tick.intervalo:PT5M}", initialDelayString = "${app.tick.intervalo:PT5M}")
    void ejecutar() {
        tick.ejecutar();
    }
}
