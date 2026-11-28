package ar.turnos.shared.tick;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Única entrada del trabajo periódico (design D2): la llaman la tarea programada y, cuando exista, el endpoint
 * del tick externo (C-03). Cada tarea corre aislada: el fallo de una no impide las demás.
 */
@Component
public class TickRunner {

    private static final Logger log = LoggerFactory.getLogger(TickRunner.class);

    private final List<TareaPeriodica> tareas;

    public TickRunner(List<TareaPeriodica> tareas) {
        this.tareas = tareas;
    }

    public void ejecutar() {
        for (TareaPeriodica tarea : tareas) {
            try {
                tarea.ejecutar();
            } catch (RuntimeException e) {
                // Sin e.getMessage(): puede traer datos personales (RN-SG-06).
                log.error("Falló la tarea periódica {}: {}", tarea.nombre(), e.getClass().getSimpleName());
            }
        }
    }
}
