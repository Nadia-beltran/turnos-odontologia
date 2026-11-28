# Design

## Context

- El repo es un monorepo: `backend/` (C-01, fusionado) y `design-system/` (revisión 1, aprobada). Todavía no existe `frontend/`.
- Reglas duras del frontend (CLAUDE.md): TypeScript sin framework, sin `any`, sin `innerHTML` con datos, sin fuentes remotas ni CDN, y todos los valores visuales salen de `design-system/tokens.css`.
- DD-02 y DD-03: el frontend estático y la API se despliegan por separado, y el hosting estático reenvía `/api/*` al backend para que el navegador vea un único origen (cookie `SameSite=Lax` de primera parte, sin CORS).
- **Estado actual del CSRF en el backend**: `SecurityConfig` usa `csrf(Customizer.withDefaults())`, que guarda el token en la sesión y no emite la cookie `XSRF-TOKEN`. La KB (08 §Seguridad) prevé `CookieCsrfTokenRepository`. El cliente se construye contra ese contrato de la KB. El ajuste del backend no entra en la zona de C-02 y queda para C-06, la primera ficha con requests que modifican y sesión.
- Requisitos: ver `specs/frontend-api-client` y `specs/frontend-routing`.

## Goals / Non-Goals

**Goals:**
- Una base mínima de la que cada ficha de pantallas solo agregue rutas y vistas.
- Que las reglas duras queden respaldadas por el build (`tsc` estricto) y por la CI, no solo por la revisión.

**Non-Goals:**
- Componentes reutilizables (`shared/components/`), `fechas.ts`, manejo de estado global y autenticación en el cliente: llegan con las fichas que los necesitan.
- Linter o formateador (ESLint, Prettier): no los pide la ficha. Las dos reglas que un linter cubriría se controlan con un chequeo en la CI (decisión D6).

## Decisions

### D1. Vite + TypeScript estricto, `tsc --noEmit` antes del build
`npm run build` = `tsc --noEmit && vite build`. `tsconfig` con `strict`, `noUncheckedIndexedAccess`, `noImplicitOverride`, `noUnusedLocals` y `noUnusedParameters`, `lib` con `DOM` y `module: ESNext` con `moduleResolution: bundler`.
*Alternativa*: dejar que solo Vite (esbuild) compile. Se descartó porque esbuild no chequea tipos y el criterio de la ficha es "sin errores de tipos".

### D2. El design system se importa desde su ubicación, sin copiarlo
`main.ts` importa `../../design-system/tokens.css`, `../../design-system/brand.js` (asigna `window.BRAND`) y el logo como URL (`../../design-system/assets/logo.svg?url`). Vite los empaqueta en `dist/`. Como el directorio está fuera de `frontend/`, `vite.config.ts` habilita `server.fs.allow: ['..']` para que el servidor de desarrollo pueda servirlos.
*Alternativa*: copiar los archivos a `frontend/src/styles/`. Se descartó: tendríamos dos fuentes de los mismos valores, y el README del design system exige una sola.
El nombre de la marca se lee de `window.BRAND.name` (con su tipo declarado en `src/env.d.ts`) y se asigna con `textContent`. Así, renombrar el producto sigue siendo editar solo `brand.js`.

### D3. Router propio y chico con History API
Una tabla de rutas `{ path, layout, render }` con patrones `:param`, convertidos a expresiones regulares. Un listener delegado en `document` intercepta los clics en `<a>` del mismo origen, salvo que el enlace tenga `target`, `download` o el clic venga con teclas modificadoras o con un botón distinto del principal, y llama a `history.pushState`. `popstate` vuelve a resolver la ruta. La ruta sin coincidencia muestra la vista "no encontrada". Al navegar, el foco pasa al `<main>` (con `tabindex="-1"`) para los lectores de pantalla.
*Alternativa*: una librería de rutas (por ejemplo, navigo). Se descartó: son unas 60 líneas propias y la regla es no sumar dependencias de runtime.

### D4. Cliente HTTP: una función `api<T>()` y la clase `ApiError`
`api<T>(path, { method, body })` llama a `fetch` con `credentials: 'same-origin'`, serializa `body` como JSON y agrega `X-XSRF-TOKEN` (leído de `document.cookie`) solo en los métodos que modifican. Un 204 devuelve `undefined`. Ante una respuesta que no es 2xx, interpreta `application/problem+json` en `ApiError { status, title, detail, errors }`. Si el cuerpo no es Problem Details, usa un `detail` genérico en español. Si `fetch` lanza un error (falla de red), se rechaza con `ApiError` con `status` 0.
`path` es relativo (`/api/...`). No hay URL base configurable: el mismo origen es la decisión DD-03.

### D5. Helpers del DOM con `textContent`
`el(tag, { attrs, text, on }, ...children)` crea elementos asignando el texto solo con `textContent` y los atributos con `setAttribute`, y `mount(parent, ...children)` reemplaza el contenido con `replaceChildren`. Ningún helper usa `innerHTML`.

### D6. Reglas duras chequeadas en la CI
Un script `npm run check:reglas` falla si encuentra `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `: any`, `as any` o `<any>` en `src/`. `tsc` estricto ya impide el `any` implícito. El workflow `frontend.yml` corre en los PR que tocan `frontend/**`, `design-system/**` o el propio workflow, con Node 22 LTS y `npm ci`, el chequeo, los tests y el build.
*Alternativa*: ESLint con `no-explicit-any` y `no-restricted-properties`. Se descartó por ahora: suma varias dependencias y configuración para dos reglas. Se puede adoptar si aparecen más reglas.

### D7. Tests unitarios con Vitest y happy-dom
Tests del cliente HTTP (`fetch` simulado y cookie) y del router (resolución de rutas y parámetros, intercepción de clics, `popstate`), uno por escenario de los specs. Son dependencias de desarrollo. Las pruebas E2E contra el backend real quedan para C-03 y las fichas de pantallas.

### D8. `_redirects` como referencia, con el backend a definir
`public/_redirects` en formato Netlify:
```
/api/*  https://BACKEND_POR_DEFINIR/api/:splat  200
/*      /index.html                                200
```
C-03 elige el hosting (PA-03), reemplaza la URL o traduce el archivo al equivalente de ese hosting. Un comentario en el archivo lo indica.

### D9. Layouts base mínimos
- `layoutPublico`: header con el logo y el nombre, `main` centrado con el ancho máximo `--form-max`, primero el celular.
- `layoutPanel`: header con el logo y el nombre, `main` con el ancho máximo `--content-max`.

Los estilos viven en `src/styles/base.css` y solo usan variables de `tokens.css`. Hay dos rutas de muestra: `/` (público) y `/panel`. C-07 y C-16 las reemplazan.

## UI Mockups

- Design system revision: 1
- Decision: no (omitido por la usuaria solo en este change), 2026-11-28. Los layouts son un esqueleto (header y `main` vacío). Las pantallas reales de C-07, C-09, C-13, C-16, C-18, C-20 y C-22 siguen necesitando su mockup aprobado.
- Mockups: —
- Approval: —
- Preview: —
- Visual verify: —

## Risks / Trade-offs

- [El backend todavía no emite la cookie `XSRF-TOKEN`, así que un `POST` real fallaría con 403] → En C-02 no hay pantallas que hagan `POST`. Queda registrado como prerequisito de C-06 en el handoff.
- [`server.fs.allow: ['..']` abre todo el repo al servidor de desarrollo] → Solo afecta al servidor local de Vite, no al build. Es aceptable en desarrollo.
- [`_redirects` con una URL de marcador de posición podría desplegarse por error] → Ningún hosting está configurado todavía. C-03 la reemplaza, y el comentario del archivo lo exige.
- [El chequeo por `grep` puede dar falsos positivos, por ejemplo con "any" en un comentario] → Los patrones buscan la sintaxis exacta (`: any`, `as any`). Si molestan, se cambia a ESLint.

## Open Questions

- Versión exacta de Node en producción: depende del hosting que se elija en C-03. La CI usa Node 22 LTS.
