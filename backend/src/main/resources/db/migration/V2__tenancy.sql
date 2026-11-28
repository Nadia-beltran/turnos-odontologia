-- C-04: tablas base de tenancy (spec tenant-isolation, design D4).
-- Las columnas de reglas de reserva de consultorio las agrega V6 (C-08).
CREATE TABLE consultorio (
    id             uuid PRIMARY KEY,
    nombre         varchar(120) NOT NULL,
    slug           varchar(60)  NOT NULL UNIQUE,
    email_contacto varchar(254) NOT NULL,
    telefono       varchar(30),
    direccion      varchar(200),
    zona_horaria   varchar(50)  NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    plan           varchar(20)  NOT NULL DEFAULT 'GRATUITO' CHECK (plan IN ('GRATUITO', 'PAGO')),
    estado         varchar(30)  NOT NULL CHECK (estado IN ('PENDIENTE_VERIFICACION', 'ACTIVO', 'SUSPENDIDO')),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
    id               uuid PRIMARY KEY,
    consultorio_id   uuid REFERENCES consultorio (id),
    email            varchar(254) NOT NULL UNIQUE,
    password_hash    varchar(100) NOT NULL,
    nombre           varchar(120) NOT NULL,
    rol              varchar(20)  NOT NULL CHECK (rol IN ('ADMIN_SAAS', 'ODONTOLOGO', 'RECEPCION')),
    email_verificado boolean      NOT NULL DEFAULT false,
    activo           boolean      NOT NULL DEFAULT true,
    ultimo_login_at  timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT usuario_rol_consultorio CHECK ((rol = 'ADMIN_SAAS') = (consultorio_id IS NULL))
);
CREATE INDEX usuario_consultorio_idx ON usuario (consultorio_id);

-- UNIQUE (consultorio_id): un solo sillón por consultorio en v1 (DD-05); también sirve de índice.
CREATE TABLE sillon (
    id             uuid PRIMARY KEY,
    consultorio_id uuid        NOT NULL UNIQUE REFERENCES consultorio (id),
    nombre         varchar(60) NOT NULL DEFAULT 'Sillón 1',
    activo         boolean     NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);
