package ar.turnos.shared.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller solo de test para ejercitar el manejo global de errores. */
@RestController
class PruebaErroresController {

    record PruebaRequest(@NotBlank String nombre) {
    }

    @PostMapping("/api/prueba/validacion")
    String validar(@Valid @RequestBody PruebaRequest request) {
        return request.nombre();
    }

    @GetMapping("/api/prueba/falla")
    String fallar() {
        throw new IllegalStateException("ERROR: relation \"paciente\" does not exist; SQL: select * from paciente where dni = '30123456'");
    }
}
