# schema-migrations Specification

## Purpose

Garantiza que el esquema de PostgreSQL se construya y evolucione solo mediante migraciones versionadas, y que el backend no arranque contra un esquema incompatible.

## Requirements

### Requirement: Migraciones aplicadas al arrancar
Al iniciar, el backend SHALL aplicar en orden todas las migraciones versionadas pendientes antes de aceptar requests, y registrar cada versión aplicada en la base.

#### Scenario: Base vacía
- **WHEN** el backend arranca contra una base de PostgreSQL vacía
- **THEN** todas las migraciones del proyecto quedan aplicadas y registradas, y el backend inicia correctamente

#### Scenario: Base al día
- **WHEN** el backend arranca contra una base que ya tiene todas las migraciones aplicadas
- **THEN** no se aplica ninguna migración y el backend inicia correctamente

### Requirement: Extensión btree_gist disponible
El esquema SHALL tener habilitada la extensión `btree_gist` desde la primera migración, porque las restricciones de exclusión de turnos dependen de ella.

#### Scenario: Primera migración
- **WHEN** se aplican las migraciones sobre una base vacía
- **THEN** la extensión `btree_gist` figura como instalada en la base

### Requirement: Esquema validado, nunca generado
El backend MUST NOT crear ni alterar tablas automáticamente desde el modelo de entidades. Si el modelo no coincide con el esquema migrado, el arranque SHALL fallar.

#### Scenario: Entidad sin migración
- **WHEN** existe una entidad mapeada cuya tabla no fue creada por ninguna migración
- **THEN** el backend no arranca e informa la discrepancia de esquema

### Requirement: Migración modificada detectada
Si una migración ya aplicada fue modificada después de aplicarse, el arranque SHALL fallar en lugar de continuar con un esquema inconsistente.

#### Scenario: Checksum distinto
- **WHEN** el contenido de una migración ya aplicada cambió respecto del registrado en la base
- **THEN** el backend no arranca e informa la migración afectada
