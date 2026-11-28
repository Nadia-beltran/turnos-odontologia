# Spec Delta

## Purpose

Ofrece un punto de entrada protegido que llama un cron externo cada 5 a 10 minutos. Mantiene despierto el backend en planes gratuitos que se apagan por inactividad y, desde C-05, dispara el procesamiento periódico de forma idempotente aunque la tarea programada interna no haya corrido.

## ADDED Requirements

### Requirement: Tick protegido por secreto
El backend SHALL aceptar `POST /api/internal/tick` solo cuando el header `X-Tick-Secret` coincide exactamente con el valor configurado del secreto del tick. Con el secreto correcto responde 204 sin cuerpo. Si el header falta o no coincide, responde 401 en formato Problem Details sin indicar cuál de los dos casos ocurrió.

#### Scenario: Secreto correcto
- **WHEN** el cron envía `POST /api/internal/tick` con `X-Tick-Secret` igual al secreto configurado
- **THEN** la respuesta tiene estado 204

#### Scenario: Secreto incorrecto
- **WHEN** un cliente envía `POST /api/internal/tick` con un `X-Tick-Secret` distinto
- **THEN** la respuesta tiene estado 401 y cuerpo `application/problem+json`

#### Scenario: Sin header
- **WHEN** un cliente envía `POST /api/internal/tick` sin `X-Tick-Secret`
- **THEN** la respuesta tiene estado 401 con el mismo `detail` que en el caso del secreto incorrecto

### Requirement: Tick sin sesión ni CSRF
`POST /api/internal/tick` SHALL autenticarse solo con el secreto: no exige sesión ni token CSRF y MUST NOT crear una sesión HTTP ni emitir cookies.

#### Scenario: Llamada de un cron sin cookies
- **WHEN** el cron externo, sin cookies ni token CSRF, envía el tick con el secreto correcto
- **THEN** la respuesta es 204 y no incluye `Set-Cookie`

### Requirement: Secreto fuera de los registros
El valor del secreto del tick y el del header recibido MUST NOT aparecer en logs, en respuestas de error ni en la auditoría. La comparación del secreto SHALL tardar lo mismo sin importar en qué posición difieren los valores.

#### Scenario: Intento con secreto incorrecto
- **WHEN** llega un tick con un secreto incorrecto
- **THEN** ni los logs ni la respuesta contienen el valor recibido ni el valor configurado

### Requirement: Tick idempotente
Recibir el tick dos veces seguidas SHALL tener el mismo efecto observable que recibirlo una vez. En este change el tick no procesa nada; los changes que le agreguen trabajo MUST conservar esta propiedad.

#### Scenario: Ticks consecutivos
- **WHEN** el cron envía dos ticks válidos seguidos
- **THEN** ambos responden 204 y el estado del sistema no cambia
