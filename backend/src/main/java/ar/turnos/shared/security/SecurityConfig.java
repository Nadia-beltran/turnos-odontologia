package ar.turnos.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import ar.turnos.shared.web.ProblemResponses;

/**
 * Seguridad inicial (design D8): toda ruta requiere autenticación, sin login de formulario
 * ni HTTP Basic, con CSRF activo. C-03 y C-06 abren explícitamente lo que corresponda.
 * Los rechazos responden en formato Problem Details (spec api-errors).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** Únicas rutas públicas por ahora (C-03, design D3). Cualquier otra ruta sigue cerrada. */
    static final String[] RUTAS_PUBLICAS_SIN_SESION = {
            "/api/health", "/api/internal/tick", "/api/internal/cookie-probe" };

    /**
     * Cadena de las rutas sin sesión: no crea ni lee sesión, no usa CSRF (no hay cookies de
     * sesión que proteger) y el tick se autentica con su propio secreto en el controller.
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher(RUTAS_PUBLICAS_SIN_SESION)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(c -> c.requireExplicitSave(true))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
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
