# Spec Delta

## Purpose

Define el formato uniforme de las respuestas de error de la API REST, para que el frontend y cualquier cliente puedan interpretar cualquier fallo de la misma manera.

## ADDED Requirements

### Requirement: Errores en formato Problem Details
La API SHALL responder todo error HTTP 4xx o 5xx con cuerpo `application/problem+json` según RFC 9457, con al menos los campos `type`, `title`, `status` y `detail`, donde `status` coincide con el código HTTP de la respuesta.

#### Scenario: Recurso inexistente
- **WHEN** un cliente autenticado pide una ruta de la API que no existe (sin sesión, la seguridad responde antes con 401 en el mismo formato)
- **THEN** la respuesta tiene estado 404, `Content-Type: application/problem+json` y un cuerpo con `status` igual a 404

#### Scenario: Error inesperado del servidor
- **WHEN** ocurre una excepción no controlada al procesar una request
- **THEN** la respuesta tiene estado 500 y cuerpo Problem Details con `status` 500

### Requirement: Errores de validación con detalle por campo
Cuando una request falla la validación de entrada, la API SHALL responder 400 en formato Problem Details e incluir una propiedad `errors` que lista, por cada campo inválido, el nombre del campo y un mensaje.

#### Scenario: Campo obligatorio faltante
- **WHEN** un cliente envía un cuerpo JSON al que le falta un campo obligatorio
- **THEN** la respuesta tiene estado 400 y `errors` contiene una entrada con el nombre de ese campo

### Requirement: Sin filtración de detalles internos
Las respuestas de error MUST NOT incluir trazas de pila, nombres de clases internas, sentencias SQL ni datos personales, cualquiera sea el perfil de ejecución.

#### Scenario: Excepción con mensaje técnico
- **WHEN** una excepción interna con un mensaje técnico (por ejemplo, de la base de datos) llega al manejador global
- **THEN** el cuerpo de la respuesta muestra un `detail` genérico y no contiene la traza ni el mensaje técnico original
