# Spec Delta

## Purpose

Deja constancia inalterable de quién hizo qué, cuándo, desde dónde y sobre qué dato, para cumplir RN-SG-02 y permitir más adelante la consulta y la exportación de la auditoría (RN-SG-04).

## ADDED Requirements

### Requirement: Registro de una acción auditada
El sistema SHALL guardar, por cada acción auditada, una fila en `auditoria` con la acción, la entidad, el id del dato, el detalle o estado previo, el tipo de actor, el usuario si lo hay, el consultorio del contexto si lo hay, la IP de la request si la hay y la fecha en UTC.

#### Scenario: Acción en el contexto de un consultorio
- **GIVEN** (DADO) una operación en el contexto del consultorio ficticio A, dentro de una request desde la IP 192.0.2.10
- **WHEN** (CUANDO) registra la acción `PRUEBA_ACCION` sobre la entidad `prueba` con un id y un detalle `{"campo":"valor-ficticio"}`
- **THEN** (ENTONCES) existe una fila de auditoría con el consultorio A, esa acción, entidad, id, detalle e IP, y una fecha en UTC

#### Scenario: Acción sin request ni consultorio
- **GIVEN** (DADO) un proceso interno sin request HTTP ni consultorio en el contexto
- **WHEN** (CUANDO) registra una acción
- **THEN** (ENTONCES) la fila queda con actor `SISTEMA`, sin consultorio, sin usuario y sin IP

#### Scenario: Acción sin nombre
- **WHEN** (CUANDO) se intenta registrar una acción con el nombre vacío o de más de 60 caracteres
- **THEN** (ENTONCES) el registro se rechaza y no se guarda ninguna fila

### Requirement: Auditoría atómica con la acción
El registro de auditoría SHALL confirmarse o revertirse junto con la operación que lo origina: no puede quedar una acción sin su registro ni un registro de una acción revertida.

#### Scenario: Operación revertida
- **GIVEN** (DADO) una operación que registra una acción de auditoría y después falla
- **WHEN** (CUANDO) su transacción se revierte
- **THEN** (ENTONCES) no queda ninguna fila de auditoría de esa acción

### Requirement: Actor coherente con el usuario
Una fila de auditoría MUST tener usuario si y solo si su actor es `USUARIO`. Los actores `PACIENTE` y `SISTEMA` no llevan usuario.

#### Scenario: Usuario faltante
- **WHEN** (CUANDO) se guarda una fila con actor `USUARIO` sin usuario
- **THEN** (ENTONCES) la base la rechaza

### Requirement: Auditoría inalterable
Una fila de auditoría MUST NOT poder modificarse ni borrarse una vez guardada, ni desde la aplicación ni con una sentencia SQL directa.

#### Scenario: Intento de modificación
- **GIVEN** (DADO) una fila de auditoría existente
- **WHEN** (CUANDO) se ejecuta un UPDATE sobre ella
- **THEN** (ENTONCES) la base rechaza la sentencia y la fila queda igual

#### Scenario: Intento de borrado
- **GIVEN** (DADO) una fila de auditoría existente
- **WHEN** (CUANDO) se ejecuta un DELETE sobre ella
- **THEN** (ENTONCES) la base rechaza la sentencia y la fila sigue existiendo

### Requirement: Sin datos sensibles fuera de la auditoría
Los registros de auditoría MUST NOT escribirse en los logs de la aplicación con su detalle. En los logs solo pueden figurar identificadores (RN-SG-06).

#### Scenario: Registro con detalle
- **WHEN** (CUANDO) se registra una acción cuyo detalle contiene datos de un paciente ficticio
- **THEN** (ENTONCES) el log de la aplicación no contiene ese detalle
