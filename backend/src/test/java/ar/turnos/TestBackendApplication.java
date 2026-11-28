package ar.turnos;

import org.springframework.boot.SpringApplication;

import ar.turnos.shared.TestcontainersConfiguration;

/**
 * Arranca el backend en local con un PostgreSQL de Testcontainers (requiere Docker):
 * ./gradlew bootTestRun
 */
public class TestBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(BackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
