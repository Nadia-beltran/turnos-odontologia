package ar.turnos.shared;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Spec schema-migrations, escenario "Checksum distinto": si una migración ya aplicada
 * se modifica, la validación falla en lugar de continuar con un esquema inconsistente.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayChecksumIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES);

    @TempDir
    Path migraciones;

    @Test
    void unaMigracionModificadaDespuesDeAplicarseHaceFallarLaValidacion() throws Exception {
        Path v1 = migraciones.resolve("V1__prueba.sql");
        Files.writeString(v1, "create table prueba_checksum (id int);");
        flyway().migrate();

        Files.writeString(v1, "create table prueba_checksum (id bigint);");

        assertThatThrownBy(() -> flyway().validate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("checksum");
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migraciones.toAbsolutePath())
                .load();
    }
}
