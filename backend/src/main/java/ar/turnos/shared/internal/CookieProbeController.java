package ar.turnos.shared.internal;

import java.time.Duration;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sonda de cookie de primera parte (spec service-health, C-03 design D6). Permite comprobar
 * a mano que una cookie emitida por el backend queda en el origen del frontend a través del
 * reenvío de /api. No crea sesión ni guarda datos.
 */
@RestController
public class CookieProbeController {

    static final String COOKIE = "cookie_probe";

    @GetMapping("/api/internal/cookie-probe")
    public ResponseEntity<Map<String, Boolean>> probe(HttpServletRequest request,
            @CookieValue(name = COOKIE, required = false) String actual) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE, "1")
                .path("/api")
                .maxAge(Duration.ofSeconds(300))
                .httpOnly(true)
                .sameSite("Lax")
                .secure(request.isSecure())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("received", actual != null));
    }
}
