package ar.turnos.shared.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tick externo (spec internal-tick, C-03 design D4). Lo llama un cron cada 10 minutos para
 * mantener despierto el servicio. En C-03 no procesa nada; C-05 y C-15 le agregan trabajo
 * idempotente. Se autentica solo con el header {@code X-Tick-Secret}: sin sesión ni CSRF.
 */
@RestController
public class TickController {

    public static final String DETALLE_RECHAZO = "Secreto del tick ausente o inválido.";

    private static final Logger log = LoggerFactory.getLogger(TickController.class);

    private final byte[] secreto;

    /** Sin secreto configurado (por ejemplo, en dev) todo tick se rechaza. */
    public TickController(@Value("${app.internal-tick-secret:}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/internal/tick")
    public ResponseEntity<ProblemDetail> tick(
            @RequestHeader(name = "X-Tick-Secret", required = false) String recibido) {
        if (!valido(recibido)) {
            // Nunca registrar el valor recibido ni el configurado (RN-SG-06).
            log.warn("Tick rechazado");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETALLE_RECHAZO))
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    private boolean valido(String recibido) {
        if (secreto.length == 0 || recibido == null) {
            return false;
        }
        // Comparación en tiempo constante respecto del contenido.
        return MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8));
    }
}
