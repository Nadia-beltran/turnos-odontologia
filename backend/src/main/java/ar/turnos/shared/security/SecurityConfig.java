package ar.turnos.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import ar.turnos.shared.web.ProblemResponses;

/**
 * Seguridad inicial (design D8): toda ruta requiere autenticación, sin login de formulario
 * ni HTTP Basic, con CSRF activo. C-03 y C-06 abren explícitamente lo que corresponda.
 * Los rechazos responden en formato Problem Details (spec api-errors).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> ProblemResponses.write(
                                request, response, HttpStatus.UNAUTHORIZED, "Se requiere iniciar sesión."))
                        .accessDeniedHandler((request, response, e) -> ProblemResponses.write(
                                request, response, HttpStatus.FORBIDDEN, "No tenés permiso para esta acción.")));
        return http.build();
    }
}
