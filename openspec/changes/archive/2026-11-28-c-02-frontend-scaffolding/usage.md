# Evidencia de uso: C-02 frontend-scaffolding

**Estado de la evidencia: incompleta, aceptada por la usuaria (Nadia, m1) el 2026-11-28 para archivar el change.**

## Costo y tiempo reportados

Indicador del cliente Claude Code informado por la usuaria al cierre: `($7.53   last $0.17  39m 31s)`.

| Dato | Valor | Fuente |
|---|---|---|
| Costo acumulado de la sesión | USD 7,53 | Indicador de costo del cliente Claude Code, informado por la usuaria |
| Costo del último turno | USD 0,17 | Ídem |
| Duración (tercer valor del indicador) | 39 min 31 s | Ídem. Se lee igual que en C-01 (duración del último turno), sin confirmar |

## Tiempo

| Bloque | Inicio (UTC) | Fin (UTC) | Fuente |
|---|---|---|---|
| Implementación (tareas 1 a 7.1) | 2026-11-28T17:40:35Z | 2026-11-28T17:55:23Z | Hora UTC registrada al iniciar y pausar |
| PR, CI y tarea 7.2 | 2026-11-28T17:55:23Z | 2026-11-28T18:03:17Z | Ídem |

La propuesta (exploración y artefactos) se hizo en la misma sesión, antes del primer bloque, sin hora de inicio registrada.

## Modelo

| Modelo | Uso |
|---|---|
| claude-opus-5-5 | Propuesta, implementación, PR, verificación y cierre (sesión única de Claude Code) |

## Integración

| PR | Head revisado | Merge | Verificación |
|---|---|---|---|
| #4 `C-02-frontend-scaffolding` | `c3b2f34` | `2172723` | `git merge-base --is-ancestor 2172723 origin/main` |

El PR se fusionó sin revisor asignado, por decisión de la usuaria (m1) del 2026-11-28.

## Limitaciones (por qué es incompleta)

- **Sin contadores de tokens**: `CLAUDE_CODE_ENABLE_TELEMETRY=1` estaba activa, pero `OTEL_LOGS_EXPORTER` no estaba configurado. Los eventos `claude_code.api_request` no se exportaron a ningún colector, así que no hay entrada, salida ni caché por modelo.
- **Sesión compartida**: la misma sesión incluyó trabajo previo de cierre de C-01. El costo de USD 7,53 es de toda la sesión y no solo de C-02. Además, el indicador puede incluir costo de sesiones anteriores si el cliente lo acumula.
- **Sin validación automática**: el validador de uso de `active-delivery` requiere Python 3.8+, que no está instalado.

## Para las próximas fichas

Configurar además `OTEL_LOGS_EXPORTER` (por ejemplo, `console` redirigido a un archivo, u `otlp` hacia un colector local) antes de abrir la sesión, e instalar Python para correr el validador.
