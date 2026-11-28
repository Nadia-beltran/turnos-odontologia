-- Solo tests (design D6): tabla para probar BaseTenantEntity sin entidades de dominio.
CREATE TABLE prueba_tenant (
    id             uuid PRIMARY KEY,
    consultorio_id uuid         NOT NULL REFERENCES consultorio (id),
    valor          varchar(80)  NOT NULL UNIQUE
);
CREATE INDEX prueba_tenant_consultorio_idx ON prueba_tenant (consultorio_id);
