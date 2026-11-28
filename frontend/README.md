# Frontend: Estudio Sonrisa

TypeScript sin framework con Vite. Los estilos, el nombre y el logo se toman de `../design-system/`, sin copiarlos.

## Comandos

Todos se corren desde `frontend/`:

```sh
npm ci                 # instalar dependencias (solo de desarrollo)
npm run dev            # servidor en http://localhost:5173; /api se reenvía a http://localhost:8080
npm test               # tests unitarios (Vitest + happy-dom)
npm run check:reglas   # falla si hay innerHTML/outerHTML/insertAdjacentHTML o `any` explícito en src/
npm run build          # tsc --noEmit (tipos estrictos) + vite build → dist/
```

## Estructura

- `src/main.ts`: arranque. Importa el design system y declara las rutas.
- `src/router.ts`: router con History API, rutas con `:param` y vista "no encontrada".
- `src/layouts.ts`: los layouts `layoutPublico` (primero el celular) y `layoutPanel`.
- `src/shared/api.ts`: `api()` y `ApiError` (cookie de sesión, `X-XSRF-TOKEN` y Problem Details).
- `src/shared/dom.ts`: `el()` y `mount()`. El texto siempre se asigna con `textContent`.
- `public/_redirects`: `/api/*` va al backend y `/*` a `index.html`. La URL del backend la define C-03.

## Pendiente: cookie `XSRF-TOKEN`

El cliente envía `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`. Hoy el backend guarda el token CSRF en la sesión y no emite esa cookie, así que cualquier `POST`, `PUT`, `PATCH` o `DELETE` responde 403. C-06 tiene que configurar `CookieCsrfTokenRepository` (KB 08 §Seguridad) antes de la primera pantalla que envíe datos.
