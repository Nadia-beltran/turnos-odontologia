package ar.turnos.shared.tick;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.test.context.TestPropertySource;

import ar.turnos.shared.AbstractIntegrationTest;

/** Spec periodic-tasks: con la ejecución programada activa, el tick corre cada 5 minutos. */
@TestPropertySource(properties = "app.tick.programado=true")
class TickProgramadoActivoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext contexto;
    @Autowired
    ScheduledAnnotationBeanPostProcessor programador;

    @Test
    void existeLaTareaProgramadaCadaCincoMinutos() {
        assertThat(contexto.getBeansOfType(TickProgramado.class)).hasSize(1);
        assertThat(programador.getScheduledTasks()).extracting(t -> ((FixedDelayTask) t.getTask()).getIntervalDuration())
                .containsExactly(Duration.ofMinutes(5));
    }
}
