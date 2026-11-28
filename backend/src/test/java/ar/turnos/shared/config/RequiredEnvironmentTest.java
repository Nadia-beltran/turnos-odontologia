package ar.turnos.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RequiredEnvironmentTest {

    @Test
    void listaSoloLasVariablesFaltantes() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_USERNAME", "app")
                .withProperty("SPRING_DATASOURCE_PASSWORD", "  ");

        List<String> faltantes = RequiredEnvironment.missing(env,
                List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"));

        assertThat(faltantes).containsExactly("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void elMensajeNombraLaVariableFaltante() {
        MockEnvironment env = new MockEnvironment();

        assertThatThrownBy(() -> RequiredEnvironment.check(env, List.of("SPRING_DATASOURCE_URL"), "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("perfil 'prod'")
                .hasMessageContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    void conTodasLasVariablesNoFalla() {
        MockEnvironment env = new MockEnvironment().withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db/x");

        assertThatCode(() -> RequiredEnvironment.check(env, List.of("SPRING_DATASOURCE_URL"), "prod"))
                .doesNotThrowAnyException();
    }
}
