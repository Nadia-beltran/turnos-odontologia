# Tasks

## 1. Proyecto Vite + TypeScript

- [x] 1.1 Crear `frontend/` con `package.json` (scripts `dev`, `build` = `tsc --noEmit && vite build`, `test`, `check:reglas`), `index.html`, `tsconfig.json` estricto (design D1) y `src/main.ts`. Instalar `vite`, `typescript`, `vitest` y `happy-dom` como dependencias de desarrollo, y verificar que `npm run build` termina bien con un `main.ts` mínimo
- [x] 1.2 Agregar `vite.config.ts` con el proxy de `/api` a `http://localhost:8080` y `server.fs.allow: ['..']` (design D2), más la configuración de Vitest con `happy-dom`. Verificar que `npm run dev` arranca y responde en `/`
- [x] 1.3 Verificar que `frontend/node_modules/` y `frontend/dist/` quedan ignorados por git (`git status` no los lista) y que `package-lock.json` sí se versiona

## 2. Helpers del DOM y reglas duras

- [x] 2.1 Implementar `src/shared/dom.ts` con `el()` y `mount()` usando `textContent`, `setAttribute` y `replaceChildren` (design D5). Verificar con un test que un texto con `<script>` queda como texto literal y no crea elementos
- [x] 2.2 Implementar el script `check:reglas` (design D6) y verificar que falla con un archivo de prueba que contiene `innerHTML` y que pasa sobre `src/` limpio

## 3. Cliente HTTP (spec `frontend-api-client`)

- [x] 3.1 Implementar `src/shared/api.ts` con `api<T>()` y `ApiError` (design D4)
- [x] 3.2 Escribir `api.test.ts` con un test por escenario del spec (mismo origen y cookies; header CSRF en POST con cookie, sin header en GET, POST sin cookie; 204 sin valor; 400 con `errors`; 502 sin Problem Details; falla de red con `status` 0). Verificar que `npm test` pasa

## 4. Router (spec `frontend-routing`)

- [x] 4.1 Implementar `src/router.ts`: tabla de rutas con `:param`, layout por ruta, intercepción de clics, `popstate`, vista "no encontrada" y foco en `<main>` al navegar (design D3)
- [x] 4.2 Escribir `router.test.ts` con un test por escenario del spec (parámetro `slug`, clic interno sin recarga, atrás del navegador, Ctrl+clic sin interceptar, enlace con `target` sin interceptar, ruta inexistente con enlace a `/`). Verificar que `npm test` pasa

## 5. Design system y layouts base

- [x] 5.1 Importar en `main.ts` `tokens.css`, `brand.js` y el logo desde `design-system/` (design D2), y declarar el tipo de `window.BRAND` en `src/env.d.ts`. Verificar que `npm run build` incluye el CSS de los tokens y el SVG en `dist/`
- [x] 5.2 Crear `src/styles/base.css` (solo con variables de `tokens.css`) y los layouts `publico` y `panel` con header (logo y nombre de marca por `textContent`) y `main` (design D9), con las rutas de muestra `/` y `/panel`. Verificar con `npm run dev` en el navegador, a 390 y 1440 px, que las dos rutas muestran su layout, que `/no-existe` muestra "no encontrada" y que la consola no tiene errores
- [x] 5.3 Verificar con un `grep` que `src/styles/` no tiene colores (`#`, `rgb(`), tamaños en `px` ni familias de fuentes escritos a mano, salvo bordes de 1 px

## 6. Hosting y CI

- [x] 6.1 Crear `frontend/public/_redirects` con las reglas de design D8 y un comentario que pide reemplazar la URL en C-03. Verificar que `npm run build` lo copia a `dist/_redirects`
- [x] 6.2 Crear `.github/workflows/frontend.yml` (Node 22, `npm ci`, `check:reglas`, `test` y `build`, con filtro de rutas `frontend/**`, `design-system/**` y el workflow, igual que `backend.yml`). Verificar la sintaxis localmente y que el job pasa en el PR
- [x] 6.3 Agregar a `frontend/README.md` cómo correr dev, build y tests, y la nota sobre la cookie `XSRF-TOKEN` pendiente para C-06. Verificar que los comandos documentados corren tal cual

## 7. Verificación integral

- [x] 7.1 Desde un clon limpio del worktree, correr `npm ci && npm run check:reglas && npm test && npm run build` en `frontend/` y verificar que todo pasa sin errores de tipos
- [ ] 7.2 Con el backend levantado en el perfil `dev`, verificar que `vite dev` reenvía `/api/cualquier-ruta` al backend (la respuesta es un 401 Problem Details del backend y no `index.html`)

## Workflow follow-up

- Crear y mantener el handoff `knowledge-base/entrega/C-02.md` (Active Delivery).
- PR con la plantilla del repo y revisión del responsable de la siguiente ficha.
- Al fusionar: marcar `[x]` en `CHANGES.md`, sincronizar los specs, registrar la evidencia de uso y archivar el change.
