package ar.turnos.shared.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * En el perfil prod verifica las variables obligatorias antes de crear cualquier bean
 * (datasource, mail, Flyway), para que el error nombre la variable faltante (design D6).
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
class ProdEnvironmentCheck {

    @Bean
    static BeanFactoryPostProcessor requiredEnvironmentCheck(Environment env) {
        return beanFactory -> RequiredEnvironment.check(env, RequiredEnvironment.PROD, "prod");
    }
}
