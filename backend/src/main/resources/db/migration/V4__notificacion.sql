-- C-05: outbox de emails (spec email-notifications, design D1).
-- consultorio_id es nullable: los emails del sistema (p. ej. recuperación de contraseña del ADMIN_SAAS) no tienen consultorio.
-- Un tipo nuevo se suma con una migración que reemplaza notificacion_tipo_check.
CREATE TABLE notificacion (
    id              uuid PRIMARY KEY,
    consultorio_id  uuid REFERENCES consultorio (id),
    tipo            varchar(40)  NOT NULL CONSTRAINT notificacion_tipo_check CHECK (tipo IN (
                        'VERIFICACION_EMAIL', 'RECUPERACION_CONTRASENA', 'OFERTA_HUECO',
                        'RESERVA_CONFIRMADA', 'TURNO_CANCELADO', 'TURNO_REPROGRAMADO',
                        'RECORDATORIO', 'INSCRIPCION_ESPERA', 'OFERTA_TOMADA')),
    prioridad       smallint     NOT NULL CHECK (prioridad BETWEEN 1 AND 4),
    destinatario    varchar(254) NOT NULL,
    payload         jsonb        NOT NULL,
    estado          varchar(20)  NOT NULL CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
    intentos        smallint     NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    programada_para timestamptz  NOT NULL,
    enviada_at      timestamptz,
    ultimo_error    text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT notificacion_enviada CHECK ((estado = 'ENVIADA') = (enviada_at IS NOT NULL))
);
CREATE INDEX notificacion_estado_programada_idx ON notificacion (estado, programada_para);
CREATE INDEX notificacion_consultorio_idx ON notificacion (consultorio_id);
