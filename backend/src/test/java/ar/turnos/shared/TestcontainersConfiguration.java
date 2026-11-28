package ar.turnos.shared;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL y Mailpit reales para tests (regla dura: nada de H2 ni mocks de la base). Misma versión mayor que producción.
 * Spring cachea el contexto, así que los contenedores se comparten entre las clases de test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final DockerImageName POSTGRES = DockerImageName.parse("postgres:15");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES);
    }

    @Bean
    MailpitContainer mailpitContainer() {
        return new MailpitContainer();
    }

    @Bean
    DynamicPropertyRegistrar mailpitProperties(MailpitContainer mailpit) {
        return registry -> {
            registry.add("spring.mail.host", mailpit::smtpHost);
            registry.add("spring.mail.port", mailpit::smtpPort);
        };
    }

    @Bean
    MailpitClient mailpitClient(MailpitContainer mailpit) {
        return new MailpitClient(mailpit.apiUrl());
    }
}
