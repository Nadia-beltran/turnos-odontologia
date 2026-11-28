package ar.turnos.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import ar.turnos.shared.security.SecurityConfig;

/** Spec api-errors y seguridad inicial (design D4 y D8). Slice web, sin base de datos. */
@WebMvcTest(controllers = PruebaErroresController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class ApiProblemDetailsTest {

    @Autowired
    MockMvc mvc;

    @Test
    @WithMockUser
    void rutaInexistenteResponde404ProblemDetails() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.type").exists());
    }

    @Test
    @WithMockUser
    void campoObligatorioFaltanteResponde400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/prueba/validacion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.DETALLE_VALIDACION))
                .andExpect(jsonPath("$.errors[0].field").value("nombre"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @WithMockUser
    void errorInternoResponde500SinDetallesTecnicos() throws Exception {
        mvc.perform(get("/api/prueba/falla"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.DETALLE_ERROR_INTERNO))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("30123456"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at ar.turnos"))));
    }

    @Test
    void sinSesionCualquierRutaResponde401ProblemDetails() throws Exception {
        mvc.perform(get("/api/prueba/falla"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/prueba/falla"));
    }

    @Test
    @WithMockUser
    void postSinTokenCsrfResponde403ProblemDetails() throws Exception {
        mvc.perform(post("/api/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Lucía\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }
}
