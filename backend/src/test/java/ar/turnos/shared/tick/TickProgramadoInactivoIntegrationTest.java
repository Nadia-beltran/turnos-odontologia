package ar.turnos.shared.tick;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ar.turnos.shared.AbstractIntegrationTest;

/** Spec periodic-tasks: los tests desactivan la ejecución programada (app.tick.programado=false). */
class TickProgramadoInactivoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext contexto;

    @Test
    void conLaPropiedadEnFalseNoHayTareaProgramada() {
        assertThat(contexto.getBeansOfType(TickProgramado.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
    }
}
