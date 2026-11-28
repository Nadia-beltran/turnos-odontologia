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

    /** Valores ficticios para todas las variables obligatorias de prod. */
    private static MockEnvironment prodCompleto() {
        MockEnvironment env = new MockEnvironment();
        RequiredEnvironment.PROD.forEach(nombre -> env.setProperty(nombre, "valor-ficticio-" + nombre));
        return env;
    }

    @Test
    void prodCompletoSinSmtpNoFalla() {
        MockEnvironment env = prodCompleto();

        assertThat(RequiredEnvironment.PROD).noneMatch(nombre -> nombre.startsWith("SPRING_MAIL_"));
        assertThatCode(() -> RequiredEnvironment.check(env, RequiredEnvironment.PROD, "prod"))
                .doesNotThrowAnyException();
    }

    @Test
    void prodSinSecretoDelTickFallaNombrandolo() {
        MockEnvironment env = prodCompleto();
        env.setProperty("APP_INTERNAL_TICK_SECRET", "");

        assertThatThrownBy(() -> RequiredEnvironment.check(env, RequiredEnvironment.PROD, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_INTERNAL_TICK_SECRET");
    }

    @Test
    void prodConDosFaltantesLasNombraSinMostrarValores() {
        MockEnvironment env = prodCompleto();
        env.setProperty("APP_BASE_URL", "");
        env.setProperty("APP_ADMIN_PASSWORD", " ");

        assertThatThrownBy(() -> RequiredEnvironment.check(env, RequiredEnvironment.PROD, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BASE_URL")
                .hasMessageContaining("APP_ADMIN_PASSWORD")
                .hasMessageNotContaining("valor-ficticio");
    }

    @Test
    void conTodasLasVariablesNoFalla() {
        MockEnvironment env = new MockEnvironment().withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db/x");

        assertThatCode(() -> RequiredEnvironment.check(env, List.of("SPRING_DATASOURCE_URL"), "prod"))
                .doesNotThrowAnyException();
    }
}
