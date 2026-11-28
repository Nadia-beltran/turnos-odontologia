package ar.turnos.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Design D4: la prioridad sale del tipo (KB 11) y se guarda al encolar. */
class TipoNotificacionTest {

    @Test
    void prioridadDeCadaTipo() {
        Map<TipoNotificacion, Integer> esperado = Map.of(
                TipoNotificacion.VERIFICACION_EMAIL, 1,
                TipoNotificacion.RECUPERACION_CONTRASENA, 1,
                TipoNotificacion.OFERTA_HUECO, 1,
                TipoNotificacion.RESERVA_CONFIRMADA, 2,
                TipoNotificacion.TURNO_CANCELADO, 2,
                TipoNotificacion.TURNO_REPROGRAMADO, 2,
                TipoNotificacion.RECORDATORIO, 3,
                TipoNotificacion.INSCRIPCION_ESPERA, 4,
                TipoNotificacion.OFERTA_TOMADA, 4);
        assertThat(esperado).hasSameSizeAs(TipoNotificacion.values());
        esperado.forEach((tipo, prioridad) -> assertThat(tipo.prioridad()).as(tipo.name()).isEqualTo(prioridad));
    }

    @Test
    void cadaTipoTieneAsuntoYNombreDePlantilla() {
        for (TipoNotificacion t : TipoNotificacion.values()) {
            assertThat(t.asunto()).as(t.name()).isNotBlank();
            assertThat(t.plantilla()).isEqualTo(t.name().toLowerCase());
        }
    }
}
