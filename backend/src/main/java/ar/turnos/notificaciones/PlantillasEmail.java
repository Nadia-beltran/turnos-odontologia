package ar.turnos.notificaciones;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renderiza el HTML y el texto plano de cada tipo con Thymeleaf núcleo, sin integración MVC (design D6).
 * Las variables son el payload más {@code consultorio}, {@code marca} y {@code baseUrl}.
 */
@Component
public class PlantillasEmail {

    private static final String CARPETA = "templates/email/";

    private final TemplateEngine motor = new TemplateEngine();
    private final String marca;
    private final String baseUrl;

    public PlantillasEmail(@Value("${app.brand-name}") String marca, @Value("${app.base-url}") String baseUrl) {
        this.marca = marca;
        this.baseUrl = baseUrl;
        motor.addTemplateResolver(resolver("*.html", TemplateMode.HTML, 1));
        motor.addTemplateResolver(resolver("*.txt", TemplateMode.TEXT, 2));
    }

    private static ClassLoaderTemplateResolver resolver(String patron, TemplateMode modo, int orden) {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix(CARPETA);
        r.setResolvablePatterns(Set.of(patron));
        r.setTemplateMode(modo);
        r.setCharacterEncoding("UTF-8");
        r.setCheckExistence(true);
        r.setOrder(orden);
        return r;
    }

    /** Un tipo se puede encolar solo si tiene sus dos plantillas. */
    public boolean existen(TipoNotificacion tipo) {
        return new ClassPathResource(CARPETA + tipo.plantilla() + ".html").exists()
                && new ClassPathResource(CARPETA + tipo.plantilla() + ".txt").exists();
    }

    /** @param consultorio nombre del consultorio, o {@code null} para un email del sistema */
    public ContenidoEmail render(TipoNotificacion tipo, Map<String, Object> payload, String consultorio) {
        Map<String, Object> variables = new HashMap<>(payload);
        variables.put("consultorio", consultorio);
        variables.put("marca", marca);
        variables.put("baseUrl", baseUrl);
        try {
            return new ContenidoEmail(
                    motor.process(tipo.plantilla() + ".html", new Context(null, variables)),
                    motor.process(tipo.plantilla() + ".txt", new Context(null, variables)));
        } catch (TemplateInputException e) {
            throw new IllegalStateException("No se pudo renderizar el tipo " + tipo, e);
        }
    }
}
