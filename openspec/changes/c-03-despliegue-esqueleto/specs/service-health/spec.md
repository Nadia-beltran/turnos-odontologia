# Spec Delta

## Purpose

Hace observable desde afuera que el backend está en pie y que el reenvío del mismo origen funciona en producción, incluida la entrega de cookies de primera parte (DD-03).

## ADDED Requirements

### Requirement: Endpoint público de salud
El backend SHALL responder `GET /api/health` sin sesión ni token CSRF, con estado 200 y un cuerpo JSON `{"status":"UP"}`. La respuesta MUST NOT incluir versión, nombres de host, datos de conexión, variables de entorno ni datos de consultorios o pacientes.

#### Scenario: Consulta sin sesión
- **WHEN** un cliente sin cookies pide `GET /api/health`
- **THEN** la respuesta tiene estado 200, `Content-Type` JSON y el cuerpo `{"status":"UP"}`

#### Scenario: A través del reenvío del frontend
- **WHEN** el navegador pide `/api/health` al origen público del frontend en producción
- **THEN** recibe la misma respuesta 200 del backend, no `index.html`

### Requirement: Salud sin dependencia de la base
`GET /api/health` SHALL responder sin hacer consultas a la base de datos, para que las llamadas periódicas no consuman la cuota de cómputo de la base gratuita.

#### Scenario: Llamadas repetidas
- **WHEN** el *health check* del hosting pide `GET /api/health` muchas veces
- **THEN** cada respuesta es 200 y ninguna de esas requests abre una conexión a la base

### Requirement: Sonda de cookie de primera parte
El backend SHALL exponer `GET /api/internal/cookie-probe` sin sesión. La respuesta emite la cookie `cookie_probe` con `Path=/api`, `HttpOnly`, `SameSite=Lax`, `Max-Age` de 300 segundos y `Secure` cuando la request llegó por HTTPS. El cuerpo JSON indica con `{"received": true|false}` si la request ya traía esa cookie. La sonda MUST NOT crear una sesión HTTP.

#### Scenario: Primera visita
- **WHEN** un navegador sin la cookie pide `GET /api/internal/cookie-probe` al origen del frontend
- **THEN** la respuesta es 200 con `{"received": false}` y un `Set-Cookie` de `cookie_probe` con `SameSite=Lax` y `HttpOnly`

#### Scenario: Segunda visita en el mismo origen
- **WHEN** el mismo navegador repite la request dentro de los 300 segundos
- **THEN** la respuesta es 200 con `{"received": true}`, lo que prueba que la cookie quedó asociada al origen del frontend
