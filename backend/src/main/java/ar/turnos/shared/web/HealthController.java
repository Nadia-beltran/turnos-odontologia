package ar.turnos.shared.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salud pública del backend (spec service-health). No consulta la base para que el
 * health check del hosting no consuma la cuota de cómputo de la base gratuita (C-03, design D5).
 */
@RestController
public class HealthController {

    private static final Map<String, String> UP = Map.of("status", "UP");

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return UP;
    }
}
