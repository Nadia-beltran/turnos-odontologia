package ar.turnos.auditoria;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AuditoriaConfig {

    @Bean
    @ConditionalOnMissingBean(ActorAuditoriaProvider.class)
    ActorAuditoriaProvider actorSistemaPorDefecto() {
        return () -> ActorAuditoria.SISTEMA;
    }
}
