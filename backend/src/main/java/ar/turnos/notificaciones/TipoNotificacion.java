package ar.turnos.notificaciones;

/**
 * Tipos de email del sistema. La prioridad (1 a 4, KB 11) determina el orden de envío y qué se sacrifica
 * al acercarse a la cuota diaria. Un tipo nuevo exige su par de plantillas y una migración que
 * reemplace {@code notificacion_tipo_check}.
 */
public enum TipoNotificacion {

    VERIFICACION_EMAIL(1, "Confirmá tu email"),
    RECUPERACION_CONTRASENA(1, "Recuperá tu contraseña"),
    OFERTA_HUECO(1, "Se liberó un turno para vos"),
    RESERVA_CONFIRMADA(2, "Tu turno está confirmado"),
    TURNO_CANCELADO(2, "Tu turno fue cancelado"),
    TURNO_REPROGRAMADO(2, "Tu turno fue reprogramado"),
    RECORDATORIO(3, "Recordatorio de tu turno"),
    INSCRIPCION_ESPERA(4, "Te anotamos en la lista de espera"),
    OFERTA_TOMADA(4, "El turno que te ofrecimos ya fue tomado");

    private final int prioridad;
    private final String asunto;

    TipoNotificacion(int prioridad, String asunto) {
        this.prioridad = prioridad;
        this.asunto = asunto;
    }

    public int prioridad() {
        return prioridad;
    }

    public String asunto() {
        return asunto;
    }

    /** Nombre base de las plantillas: {@code templates/email/<plantilla>.html} y {@code .txt}. */
    public String plantilla() {
        return name().toLowerCase();
    }
}
