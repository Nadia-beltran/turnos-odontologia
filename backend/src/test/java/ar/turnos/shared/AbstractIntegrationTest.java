package ar.turnos.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para tests de integración con PostgreSQL real (design D7).
 * Cada ficha extiende esta clase para probar persistencia, restricciones y aislamiento.
 *
 * <p>Suma {@code db/testmigration} (solo tests, design D6) para la tabla {@code prueba_tenant}.
 *
 * <p>Sin Docker disponible, las clases que la extienden quedan omitidas (no fallan).
 * En CI el workflow exige Docker, así que ahí siempre corren.
 */
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/testmigration",
        // La tarea programada no debe competir con los tests (C-05 design D8).
        "app.tick.programado=false" })
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {
}
