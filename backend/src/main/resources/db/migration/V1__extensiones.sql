-- C-01: extensiones requeridas por el esquema (spec schema-migrations).
-- btree_gist habilita las restricciones de exclusión de turnos (RN-AG-01, C-10).
CREATE EXTENSION IF NOT EXISTS btree_gist;
