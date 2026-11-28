# Spec Delta

## Purpose

Garantiza que cada dato de negocio pertenezca a exactamente un consultorio y que, en el esquema compartido, ningún consultorio pueda leer ni modificar los datos de otro (RN-MT-01, RN-MT-02, DD-01).

## ADDED Requirements

### Requirement: Esquema base de consultorio, usuario y sillón
El esquema SHALL tener las tablas `consultorio`, `usuario` y `sillon` de `knowledge-base/04`. `usuario.email` y `consultorio.slug` son únicos. `sillon.consultorio_id` es obligatorio. Toda columna `consultorio_id` está indexada.

#### Scenario: Migraciones sobre base vacía
- **GIVEN** (DADO) una base de PostgreSQL vacía
- **WHEN** (CUANDO) el backend arranca y aplica las migraciones
- **THEN** (ENTONCES) existen `consultorio`, `usuario` y `sillon` con sus columnas, y el arranque valida el esquema sin errores

#### Scenario: Slug repetido
- **GIVEN** (DADO) un consultorio ficticio con slug `consultorio-prueba-a`
- **WHEN** (CUANDO) se inserta otro consultorio con el mismo slug
- **THEN** (ENTONCES) la base rechaza la inserción por violar la unicidad

### Requirement: Rol y consultorio coherentes en usuario
Un usuario MUST tener consultorio si y solo si su rol no es `ADMIN_SAAS`. La base SHALL rechazar cualquier fila que rompa esa regla.

#### Scenario: Odontólogo con consultorio
- **GIVEN** (DADO) un consultorio ficticio existente
- **WHEN** (CUANDO) se inserta un usuario `ODONTOLOGO` asociado a ese consultorio
- **THEN** (ENTONCES) la inserción se acepta

#### Scenario: Administrador con consultorio
- **GIVEN** (DADO) un consultorio ficticio existente
- **WHEN** (CUANDO) se inserta un usuario `ADMIN_SAAS` asociado a ese consultorio
- **THEN** (ENTONCES) la base rechaza la inserción

#### Scenario: Recepción sin consultorio
- **WHEN** (CUANDO) se inserta un usuario `RECEPCION` sin consultorio
- **THEN** (ENTONCES) la base rechaza la inserción

### Requirement: Un único sillón por consultorio
En v1 la base SHALL rechazar un segundo sillón para un mismo consultorio (DD-05).

#### Scenario: Segundo sillón
- **GIVEN** (DADO) un consultorio ficticio que ya tiene `Sillón 1`
- **WHEN** (CUANDO) se inserta otro sillón para ese consultorio
- **THEN** (ENTONCES) la base rechaza la inserción

### Requirement: Lecturas limitadas al consultorio actual
Toda lectura de una entidad de negocio SHALL devolver solo filas del consultorio establecido en el contexto de la operación, incluidas las búsquedas por identificador. Un dato de otro consultorio se comporta como inexistente.

#### Scenario: Listado propio
- **GIVEN** (DADO) dos consultorios ficticios A y B con datos de negocio cada uno
- **WHEN** (CUANDO) una operación en el contexto de A lista esos datos
- **THEN** (ENTONCES) obtiene solo los datos de A

#### Scenario: Búsqueda por id ajeno
- **GIVEN** (DADO) un dato de negocio del consultorio B
- **WHEN** (CUANDO) una operación en el contexto de A lo busca por su identificador
- **THEN** (ENTONCES) no lo encuentra, igual que si no existiera

### Requirement: Operación sin consultorio rechazada
Una lectura o escritura de una entidad de negocio sin consultorio establecido en el contexto, y fuera del modo sistema explícito, MUST fallar sin acceder a datos, en lugar de devolver datos de todos los consultorios.

#### Scenario: Consulta sin contexto
- **GIVEN** (DADO) datos de negocio de los consultorios A y B
- **WHEN** (CUANDO) se consulta esa entidad sin consultorio en el contexto y sin modo sistema
- **THEN** (ENTONCES) la operación falla y no devuelve ninguna fila

### Requirement: Modo sistema explícito y acotado
El sistema SHALL permitir leer entidades de negocio sin consultorio solo dentro de un bloque de modo sistema declarado de forma explícita por el código del servidor, para casos donde el consultorio todavía no se conoce (por ejemplo, buscar un usuario por email al iniciar sesión). El modo MUST terminar al salir del bloque, con o sin error.

#### Scenario: Búsqueda en modo sistema
- **GIVEN** (DADO) un dato de negocio del consultorio A con un valor único ficticio `valor-sistema-1`
- **WHEN** (CUANDO) una operación declarada explícitamente en modo sistema lo busca por ese valor, sin consultorio en el contexto
- **THEN** (ENTONCES) lo encuentra

#### Scenario: Mismo pedido sin declarar el modo
- **GIVEN** (DADO) el mismo dato del consultorio A
- **WHEN** (CUANDO) la misma búsqueda se ejecuta sin consultorio y sin declarar el modo sistema
- **THEN** (ENTONCES) la operación falla, igual que cualquier consulta sin consultorio

#### Scenario: El modo sistema no sobrevive al bloque
- **GIVEN** (DADO) un bloque en modo sistema que terminó, con o sin error
- **WHEN** (CUANDO) el mismo hilo consulta después sin consultorio
- **THEN** (ENTONCES) la operación falla

#### Scenario: Consultorio dentro del modo sistema
- **GIVEN** (DADO) un bloque en modo sistema
- **WHEN** (CUANDO) dentro de él se establece el consultorio A para una operación anidada
- **THEN** (ENTONCES) esa operación ve solo datos de A y, al terminar, el bloque vuelve al modo sistema

### Requirement: Consultorio asignado solo por el servidor
Al crear un dato de negocio, el sistema SHALL asignarle el consultorio del contexto e ignorar o rechazar cualquier otro valor. El consultorio de un dato existente MUST NOT cambiar.

#### Scenario: Alta en el contexto de A
- **WHEN** (CUANDO) una operación en el contexto de A crea un dato de negocio
- **THEN** (ENTONCES) el dato queda guardado con el consultorio A

#### Scenario: Alta con consultorio ajeno
- **WHEN** (CUANDO) una operación en el contexto de A intenta crear un dato marcado con el consultorio B
- **THEN** (ENTONCES) la operación falla y no se guarda nada

### Requirement: Modificaciones limitadas al consultorio actual
Modificar o borrar un dato de negocio SHALL estar permitido solo si el dato pertenece al consultorio del contexto. Si no, la operación MUST fallar sin cambios.

#### Scenario: Modificación ajena
- **GIVEN** (DADO) un dato de negocio del consultorio B
- **WHEN** (CUANDO) una operación en el contexto de A intenta modificarlo o borrarlo
- **THEN** (ENTONCES) la operación falla y el dato queda igual en la base

#### Scenario: Contexto restaurado
- **GIVEN** (DADO) una operación en el contexto de A que termina, con o sin error
- **WHEN** (CUANDO) el mismo hilo ejecuta otra operación sin establecer consultorio
- **THEN** (ENTONCES) no hereda el consultorio A y se rechaza como operación sin consultorio
