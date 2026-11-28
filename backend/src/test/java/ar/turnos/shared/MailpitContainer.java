package ar.turnos.shared;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Mailpit real como servidor SMTP de pruebas (design D8). Versión fija, nunca {@code latest}. */
public class MailpitContainer extends GenericContainer<MailpitContainer> {

    public static final DockerImageName IMAGEN = DockerImageName.parse("axllent/mailpit:v1.21.8");
    private static final int SMTP = 1025;
    private static final int API = 8025;

    public MailpitContainer() {
        super(IMAGEN);
        withExposedPorts(SMTP, API);
        waitingFor(Wait.forHttp("/livez").forPort(API));
    }

    public String smtpHost() {
        return getHost();
    }

    public int smtpPort() {
        return getMappedPort(SMTP);
    }

    public String apiUrl() {
        return "http://" + getHost() + ":" + getMappedPort(API);
    }
}
