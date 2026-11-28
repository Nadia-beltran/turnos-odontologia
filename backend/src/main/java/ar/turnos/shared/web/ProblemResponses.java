package ar.turnos.shared.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Escribe respuestas Problem Details (RFC 9457) fuera de Spring MVC, por ejemplo desde
 * los filtros de Spring Security, donde el {@code @RestControllerAdvice} no interviene.
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"type\":\"about:blank\""
                + ",\"title\":\"" + json(status.getReasonPhrase()) + "\""
                + ",\"status\":" + status.value()
                + ",\"detail\":\"" + json(detail) + "\""
                + ",\"instance\":\"" + json(request.getRequestURI()) + "\"}");
    }

    private static String json(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
