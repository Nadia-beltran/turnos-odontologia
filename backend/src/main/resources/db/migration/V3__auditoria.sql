-- C-04: auditoría append-only (spec audit-log, design D4).
CREATE TABLE auditoria (
    id             bigserial PRIMARY KEY,
    consultorio_id uuid REFERENCES consultorio (id),
    usuario_id     uuid REFERENCES usuario (id),
    actor_tipo     varchar(20) NOT NULL CHECK (actor_tipo IN ('USUARIO', 'PACIENTE', 'SISTEMA')),
    accion         varchar(60) NOT NULL,
    entidad        varchar(40),
    entidad_id     uuid,
    datos          jsonb,
    ip             inet,
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT auditoria_actor_usuario CHECK ((actor_tipo = 'USUARIO') = (usuario_id IS NOT NULL))
);
CREATE INDEX auditoria_consultorio_fecha_idx ON auditoria (consultorio_id, created_at DESC);

CREATE FUNCTION auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'auditoria es append-only: no se permite % (RN-CL-01)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_append_only
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_inmutable();
