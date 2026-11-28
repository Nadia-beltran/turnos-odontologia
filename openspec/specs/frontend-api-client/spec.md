# frontend-api-client Specification

## Purpose

Define cómo el frontend llama a la API REST: en el mismo origen, con la cookie de sesión y el token CSRF, y cómo convierte los errores Problem Details en un error uniforme para las pantallas.

## Requirements

### Requirement: Llamadas en el mismo origen con la cookie de sesión
El cliente de la API SHALL enviar cada request a rutas relativas bajo `/api` en el mismo origen de la página, incluyendo las cookies de ese origen, y MUST NOT enviar credenciales a otros orígenes.

#### Scenario: Request con sesión
- **WHEN** una pantalla pide `GET /api/algo` mediante el cliente
- **THEN** la request sale al mismo origen de la página e incluye las cookies de ese origen

### Requirement: Header CSRF en los métodos que modifican
En toda request con método `POST`, `PUT`, `PATCH` o `DELETE`, el cliente SHALL enviar el header `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN` cuando esa cookie exista. En `GET` y `HEAD` MUST NOT enviar ese header.

#### Scenario: POST con cookie XSRF-TOKEN presente
- **WHEN** existe la cookie `XSRF-TOKEN` con valor `abc` y una pantalla envía un `POST`
- **THEN** la request lleva el header `X-XSRF-TOKEN: abc`

#### Scenario: GET sin header CSRF
- **WHEN** una pantalla envía un `GET`
- **THEN** la request no lleva el header `X-XSRF-TOKEN`

#### Scenario: POST sin cookie
- **WHEN** no existe la cookie `XSRF-TOKEN` y una pantalla envía un `POST`
- **THEN** la request sale sin el header `X-XSRF-TOKEN` y el rechazo del servidor se informa como cualquier otro error

### Requirement: Cuerpo JSON y respuesta exitosa
Cuando una pantalla envía datos, el cliente SHALL serializarlos como JSON con `Content-Type: application/json`. Ante una respuesta 2xx SHALL devolver el cuerpo JSON interpretado, o ningún valor si la respuesta no tiene cuerpo.

#### Scenario: Respuesta 204
- **WHEN** el servidor responde 204 sin cuerpo
- **THEN** el cliente resuelve sin valor y sin error

### Requirement: Errores Problem Details como error tipado
Ante una respuesta 4xx o 5xx, el cliente SHALL rechazar con un error que expone `status`, `title`, `detail` y, si vienen, los `errors` por campo del cuerpo `application/problem+json`.

#### Scenario: Error de validación
- **WHEN** el servidor responde 400 con un Problem Details que incluye `errors` con el campo `email`
- **THEN** el error rechazado tiene `status` 400 y una entrada de error para `email`

#### Scenario: Error sin Problem Details
- **WHEN** el servidor (o un proxy intermedio) responde 502 con un cuerpo que no es `application/problem+json`
- **THEN** el error rechazado tiene `status` 502 y un `detail` genérico en español, sin exponer el cuerpo recibido

### Requirement: Falla de red informada
Si la request no llega a obtener respuesta, el cliente SHALL rechazar con el mismo tipo de error, con `status` 0 y un `detail` genérico en español.

#### Scenario: Sin conexión
- **WHEN** el navegador no puede conectar con el servidor
- **THEN** el error rechazado tiene `status` 0 y un mensaje genérico de conexión
