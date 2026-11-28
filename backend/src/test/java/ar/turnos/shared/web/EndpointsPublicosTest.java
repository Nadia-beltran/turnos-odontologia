package ar.turnos.shared.web;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ar.turnos.shared.security.SecurityConfig;
import ar.turnos.shared.internal.CookieProbeController;
import ar.turnos.shared.internal.TickController;

/**
 * Specs service-health e internal-tick (C-03, design D3 a D6). Slice web sin base de datos:
 * si el contexto arranca, ninguno de estos controllers depende de la base.
 */
@WebMvcTest(controllers = { HealthController.class, TickController.class, CookieProbeController.class,
        PruebaErroresController.class })
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
@TestPropertySource(properties = "app.internal-tick-secret=" + EndpointsPublicosTest.SECRETO)
class EndpointsPublicosTest {

    static final String SECRETO = "secreto-ficticio-de-prueba-0123456789";

    @Autowired
    MockMvc mvc;

    // --- service-health ---

    @Test
    void healthRespondeUpSinSesionNiCookies() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void sondaPrimeraVisitaEmiteCookieLax() throws Exception {
        MvcResult res = mvc.perform(get("/api/internal/cookie-probe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(false))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("cookie_probe=1"),
                        containsString("Path=/api"),
                        containsString("Max-Age=300"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"),
                        not(containsString("Secure")))))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(res.getRequest().getSession(false)).isNull();
    }

    @Test
    void sondaConLaCookieInformaRecibida() throws Exception {
        mvc.perform(get("/api/internal/cookie-probe").cookie(new Cookie("cookie_probe", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    void sondaPorHttpsMarcaLaCookieSecure() throws Exception {
        mvc.perform(get("/api/internal/cookie-probe").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    // --- internal-tick ---

    @Test
    void tickConSecretoCorrectoResponde204SinCookiesNiCsrf() throws Exception {
        MvcResult res = mvc.perform(post("/api/internal/tick").header("X-Tick-Secret", SECRETO))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(res.getRequest().getSession(false)).isNull();
    }

    @Test
    void tickConsecutivosRespondenIgual() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/internal/tick").header("X-Tick-Secret", SECRETO))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void tickConSecretoIncorrectoResponde401ProblemDetails() throws Exception {
        mvc.perform(post("/api/internal/tick").header("X-Tick-Secret", "otro-valor-incorrecto"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(TickController.DETALLE_RECHAZO))
                .andExpect(content().string(not(containsString("otro-valor-incorrecto"))))
                .andExpect(content().string(not(containsString(SECRETO))));
    }

    @Test
    void tickSinHeaderResponde401ConElMismoDetalle() throws Exception {
        mvc.perform(post("/api/internal/tick"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(TickController.DETALLE_RECHAZO));
    }

    // --- regresión: la cadena general no cambió ---

    @Test
    void otroPostSinTokenCsrfSigueRechazado() throws Exception {
        mvc.perform(post("/api/prueba/validacion").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void otraRutaBajoInternalNoQuedaAbierta() throws Exception {
        mvc.perform(get("/api/internal/no-existe"))
                .andExpect(status().isUnauthorized());
    }
}
