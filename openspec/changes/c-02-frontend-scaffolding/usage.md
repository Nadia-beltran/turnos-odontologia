# Evidencia de uso: C-02 frontend-scaffolding

**Estado de la evidencia: incompleta.** Falta el desglose de tokens. El archivo del change requiere que la usuaria acepte esta evidencia.

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
- **Sesión compartida**: la misma sesión incluyó trabajo previo de cierre de C-01.
- **Sin validación automática**: el validador de uso de `active-delivery` requiere Python 3.8+, que no está instalado.

## Para las próximas fichas

Configurar además `OTEL_LOGS_EXPORTER` (por ejemplo, `console` redirigido a un archivo, u `otlp` hacia un colector local) antes de abrir la sesión, e instalar Python para correr el validador.
