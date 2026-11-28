package ar.turnos.shared.tick;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Spec periodic-tasks: las tareas son independientes entre sí. */
@ExtendWith(OutputCaptureExtension.class)
class TickRunnerTest {

    private static TareaPeriodica tarea(String nombre, Runnable cuerpo) {
        return new TareaPeriodica() {
            @Override
            public String nombre() {
                return nombre;
            }

            @Override
            public void ejecutar() {
                cuerpo.run();
            }
        };
    }

    @Test
    void siUnaTareaFallaLaSiguienteCorreIgualYElLogTieneElNombreSinElMensaje(CapturedOutput salida) {
        List<String> corridas = new ArrayList<>();
        TareaPeriodica falla = tarea("tarea-que-falla", () -> {
            throw new IllegalStateException("dato personal ficticio paciente@example.test");
        });
        TareaPeriodica sana = tarea("tarea-sana", () -> corridas.add("sana"));

        new TickRunner(List.of(falla, sana)).ejecutar();

        assertThat(corridas).containsExactly("sana");
        assertThat(salida.getAll()).contains("tarea-que-falla").contains("IllegalStateException")
                .doesNotContain("paciente@example.test");
    }

    @Test
    void correTodasLasTareasEnElOrdenRecibido() {
        List<String> corridas = new ArrayList<>();

        new TickRunner(List.of(tarea("a", () -> corridas.add("a")), tarea("b", () -> corridas.add("b")),
                tarea("c", () -> corridas.add("c")))).ejecutar();

        assertThat(corridas).containsExactly("a", "b", "c");
    }

    static class TareaDePrueba implements TareaPeriodica {
        @Override
        public void ejecutar() {
        }
    }

    @Test
    void elNombrePorDefectoEsLaClase() {
        assertThat(new TareaDePrueba().nombre()).isEqualTo("TareaDePrueba");
    }
}
