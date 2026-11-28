package ar.turnos.shared.web;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejo global de errores de la API en formato Problem Details (spec api-errors, design D4).
 * Las excepciones estándar de Spring MVC (404, 405, 400 de lectura, etc.) las resuelve la clase base.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    static final String DETALLE_VALIDACION = "La solicitud tiene campos inválidos.";
    static final String DETALLE_ERROR_INTERNO = "Ocurrió un error inesperado. Intentá de nuevo más tarde.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setDetail(DETALLE_VALIDACION);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", Objects.requireNonNullElse(fe.getDefaultMessage(), "valor inválido")))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Todas las respuestas de error llevan {@code type} explícito (spec api-errors). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            withType(problem);
        }
        return response;
    }

    static ProblemDetail withType(ProblemDetail problem) {
        if (problem.getType() == null) {
            problem.setType(ABOUT_BLANK);
        }
        return problem;
    }

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    /**
     * Cualquier excepción no prevista: 500 con detalle genérico. El detalle técnico va solo al log,
     * nunca a la respuesta (sin trazas, SQL ni datos personales).
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex; // las resuelve Spring Security (401/403), no son errores internos
        }
        log.error("Error no controlado al procesar la request", ex);
        ProblemDetail body = withType(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, DETALLE_ERROR_INTERNO));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
