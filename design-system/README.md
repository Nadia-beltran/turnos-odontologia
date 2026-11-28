# Estudio Sonrisa: Sistema de Diseño

Mode: system-and-mockups
Revision: 1
Approval: approved (2026-11-28, by user)

**Política del proyecto**: `system-and-mockups`. Cada cambio visual posterior presenta un mockup HTML que debe aprobarse antes de implementarlo. Lo eligió la usuaria en el plan de fundación del 2026-11-28.

## Propósito

Base visual compartida para:

- El **panel del consultorio**: odontólogo y recepción, en escritorio y en el celular.
- Las **páginas públicas del paciente**: reserva, gestión del turno y lista de espera, pensadas primero para el celular.

## Archivos

| Archivo | Qué contiene | Cómo se usa |
|---|---|---|
| `tokens.css` | Color, tipografía, espaciado, radios, sombras, foco, tamaños táctiles y movimiento (con movimiento reducido) | El frontend y los mockups lo importan. **Es la fuente única de valores visuales** |
| `brand.js` | Constante `BRAND_NAME` y `BRAND_TAGLINE`, aplicadas a `[data-brand-name]` y `[data-brand-tagline]` | Los previews y mockups lo incluyen con `<script src=".../brand.js" defer>` |
| `brand.md` | Nombre, tono, uso del logo e imágenes | Referencia de redacción y marca |
| `assets/logo.svg` | Logo **provisorio** | Ruta estable: reemplazar el archivo sin cambiar el nombre |
| `components.html` | Componentes y estados representativos | Abrir en el navegador como referencia |
| `previews/` | Capturas a 390×844 y 1440×900, viewport y página completa | Revisión visual |

## Fuentes

- `knowledge-base/01_vision_y_objetivos.md`, `03_actores_y_roles.md`, `05_reglas_de_negocio.md`, `06_funcionalidades.md`: actores, estados del turno, regla de 24 h, flujos del paciente.
- `discovery/research/analisis-competitivo.md`, sección D.2: referencias de experiencia de usuario.
  - **Turnito**: reserva sin cuenta.
  - **Odonto.ar**: odontograma con estados.
  - **ixi**: gestión del turno desde el mensaje.
- `knowledge-base/09_decisiones_y_supuestos.md` (DD-14): nombre del producto.

## Principios

1. **Primero el celular en las páginas del paciente**: objetivos táctiles de al menos 44 px, inputs de 16 px (iOS no hace zoom), una acción primaria por pantalla.
2. **Calidez sin frialdad clínica**: verde azulado (`primary`) como color de acción y coral (`accent`) como guiño a la "sonrisa", siempre decorativo o con texto oscuro.
3. **El estado nunca solo por color**: cada estado de turno lleva su texto.
4. **Las acciones destructivas no son primarias** y siempre piden confirmación.
5. **Sin dependencias remotas**: solo fuentes del sistema, sin CDN ni cuentas.
6. **Movimiento breve** (120 a 280 ms), que se anula con `prefers-reduced-motion`.

## Decisiones abiertas (TBD)

| Ítem | Estado | Dónde se reemplaza |
|---|---|---|
| Logo definitivo | **(TBD)**: el actual es un marcador rotulado "LOGO PROVISORIO" | Reemplazar `assets/logo.svg` (mismo nombre de archivo) |
| Nombre | Definido: Estudio Sonrisa (DD-14). Pendiente la verificación en el INPI y NIC.ar | `brand.js` → `BRAND_NAME` |
| Tagline | Provisoria: "Turnos para tu consultorio, sin huecos vacíos" | `brand.js` → `BRAND_TAGLINE` |
| Modo oscuro | Fuera de v1 | Se agregaría como bloque `@media (prefers-color-scheme: dark)` en `tokens.css` (nueva revisión) |
| Catálogo de estados del odontograma | Provisorio (PA-05) | `components.html`, sección del odontograma, y los tokens de color si se agregan estados |

## Cómo reemplazar

- **Logo**: guardar el SVG definitivo como `design-system/assets/logo.svg`, con fondo transparente y sin texto "provisorio". Volver a capturar los previews.
- **Nombre o tagline**: editar las dos constantes de `brand.js`. En el backend, actualizar la constante de configuración equivalente de los emails (DD-14).
- **Un valor visual**: editar el token en `tokens.css`, **subir `Revision`** en este archivo, pasar `Approval` a `pending`, identificar los mockups afectados en `openspec/changes/*/mockups/` y volver a presentarlos.

## Cómo regenerar los previews

Se generan con Microsoft Edge en modo headless (lo trae Windows), sin instalar nada:

```powershell
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
& $edge --headless=new --hide-scrollbars --window-size=1440,900 --screenshot="design-system\previews\components-desktop-1440x900.png" "file:///<ruta-absoluta>/design-system/components.html"
```

Edge en modo headless no admite ventanas de menos de unos 500 px de ancho. Para 390 px, la página se carga dentro de un `iframe` de 390 px y se recorta la captura con `System.Drawing`.

## Historial

| Revisión | Fecha | Cambio | Aprobación |
|---|---|---|---|
| 1 | 2026-11-28 | Base inicial | Aprobada por la usuaria (2026-11-28) |
