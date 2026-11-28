# periodic-tasks Specification

## Purpose

Asegura que el trabajo periódico del backend (outbox de emails y, más adelante, recordatorios y vencimientos) corra cada 5 minutos y que el tick externo de *keep-alive* ejecute exactamente lo mismo, de forma idempotente (KB 12, Flujo 6).

## Requirements

### Requirement: Ejecución programada cada 5 minutos
El backend SHALL ejecutar todas las tareas periódicas registradas cada 5 minutos mientras está en marcha, salvo que la configuración desactive la ejecución programada (por ejemplo, en los tests).

#### Scenario: Ejecución programada
- **GIVEN** (DADO) el backend en marcha con la ejecución programada activa
- **WHEN** (CUANDO) se cumple el intervalo de 5 minutos
- **THEN** (ENTONCES) se ejecutan todas las tareas periódicas registradas, entre ellas el procesamiento del outbox

### Requirement: Tick equivalente a la ejecución programada
Una invocación del tick SHALL ejecutar el mismo conjunto de tareas, con el mismo efecto, que la ejecución programada. Repetir el tick MUST NOT repetir trabajo ya hecho.

#### Scenario: Tick procesa el outbox
- **GIVEN** (DADO) una notificación `PENDIENTE` vencida
- **WHEN** (CUANDO) se invoca el tick
- **THEN** (ENTONCES) la notificación queda `ENVIADA`, igual que con la ejecución programada

#### Scenario: Tick repetido
- **GIVEN** (DADO) un tick que ya envió una notificación
- **WHEN** (CUANDO) se invoca el tick otra vez
- **THEN** (ENTONCES) no se envía de nuevo

### Requirement: Tareas independientes entre sí
El fallo de una tarea periódica MUST NOT impedir que se ejecuten las demás en la misma corrida, y SHALL registrarse en el log sin datos personales.

#### Scenario: Una tarea falla
- **GIVEN** (DADO) dos tareas periódicas, la primera de las cuales lanza un error
- **WHEN** (CUANDO) corre la ejecución periódica
- **THEN** (ENTONCES) la segunda tarea se ejecuta igual y el error queda en el log con el nombre de la tarea
