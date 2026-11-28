package ar.turnos.notificaciones;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NotificacionesConfig {

    /** Reloj en UTC inyectable: el tiempo nunca sale de la hora local del servidor (RN-EX-02). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
