# Proposal

## Why

Todavía no hay frontend. Las fichas de pantallas (C-07, C-09, C-13, C-16 y siguientes) y el despliegue temprano de C-03 necesitan una base común: un build tipado, un router, un cliente HTTP que respete la sesión por cookie, el CSRF y los errores `ProblemDetail` del backend, y los tokens del design system. Si esta base se arma una sola vez, cada pantalla no reinventa esas piezas ni rompe las reglas duras (sin `innerHTML` con datos, sin frameworks, sin valores visuales a mano).

## What Changes

- Nuevo proyecto `frontend/` con Vite y TypeScript estricto sin framework. `npm run build` ejecuta `tsc --noEmit` antes del build de Vite.
- `src/router.ts`: router en el cliente con History API, enlaces internos sin recarga y una vista de "página no encontrada".
- `src/shared/api.ts`: cliente `fetch` con `credentials: 'same-origin'`, header `X-XSRF-TOKEN` en los métodos que modifican, y un error tipado a partir de las respuestas `application/problem+json`.
- `src/shared/dom.ts`: helpers para crear elementos que asignan el texto con `textContent` y nunca usan `innerHTML`.
- Import de `design-system/tokens.css` y dos layouts base mínimos: público (primero el celular) y panel. Cada uno es un esqueleto con header (logo provisorio y nombre de la marca) y un `main` vacío.
- Proxy de `vite dev` de `/api` a `http://localhost:8080`.
- `public/_redirects` con el formato de Netlify como referencia: `/api/*` al backend y `/*` a `index.html`. La URL real del backend la define C-03 (PA-03).
- CI `.github/workflows/frontend.yml`: `npm ci && npm run build` y los tests unitarios del cliente.

**Fuera de alcance**: pantallas reales (login, reserva, agenda), `fechas.ts`, la elección del hosting y la configuración de CSRF del backend. Sobre esto último, hoy el backend guarda el token CSRF en la sesión y no en una cookie: el cambio a `CookieCsrfTokenRepository` queda para C-06.

## Capabilities

### New Capabilities
- `frontend-api-client`: cómo el frontend llama a la API (mismo origen, cookie de sesión, header CSRF) y cómo convierte los errores Problem Details en un error tipado.
- `frontend-routing`: navegación del SPA con History API, resolución de rutas a vistas y vista de ruta inexistente, con *fallback* a `index.html` en el hosting.

### Modified Capabilities
<!-- Ninguna: el cliente consume `api-errors` sin cambiar sus requisitos. -->

## Impact

- **Código nuevo**: `frontend/` y `.github/workflows/frontend.yml`. No toca `backend/`.
- **Dependencias** (solo de desarrollo): `vite`, `typescript` y `vitest` para los tests unitarios del cliente y el router. No se agregan dependencias de runtime.
- **Design system**: se importa `design-system/tokens.css` y `assets/logo.svg` sin modificarlos. El mockup se omite solo en este change, por decisión de la usuaria (2026-11-28).
- **Desbloquea**: C-03 (despliegue del esqueleto) y C-07 (auth-ui-panel-shell).
