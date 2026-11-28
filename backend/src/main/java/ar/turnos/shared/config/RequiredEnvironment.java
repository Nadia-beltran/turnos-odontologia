package ar.turnos.shared.config;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Variables de entorno obligatorias por perfil (knowledge-base/08 §Variables de entorno).
 * Permite fallar al arrancar con un mensaje que nombra exactamente lo que falta.
 */
public final class RequiredEnvironment {

    /** Variables sin valor por defecto en el perfil prod. */
    public static final List<String> PROD = List.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "SPRING_MAIL_HOST",
            "SPRING_MAIL_PORT",
            "SPRING_MAIL_USERNAME",
            "SPRING_MAIL_PASSWORD",
            "APP_BASE_URL",
            "APP_MAIL_FROM",
            "APP_MAIL_DAILY_QUOTA",
            "APP_INTERNAL_TICK_SECRET",
            "APP_ADMIN_EMAIL",
            "APP_ADMIN_PASSWORD");

    private RequiredEnvironment() {
    }

    /** Devuelve las variables de {@code required} que no tienen valor en {@code env}. */
    public static List<String> missing(Environment env, List<String> required) {
        return required.stream()
                .filter(name -> !StringUtils.hasText(env.getProperty(name)))
                .toList();
    }

    /** Lanza una excepción que lista todas las variables faltantes, si las hay. */
    public static void check(Environment env, List<String> required, String profile) {
        List<String> faltantes = missing(env, required);
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan variables de entorno obligatorias para el perfil '" + profile + "': "
                            + String.join(", ", faltantes));
        }
    }
}
