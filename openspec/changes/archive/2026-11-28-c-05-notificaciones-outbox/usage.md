# Evidencia de uso: C-05 notificaciones-outbox

**Estado de la evidencia: incompleta, aceptada por la usuaria (Nadia, m1) el 2026-11-28 para archivar el change.**

## Tiempo (reconstruido desde Git y GitHub)

| Hito | Hora (UTC) | Fuente |
|---|---|---|
| Commit `feat(C-05): outbox de emails` (`7757bf4`) | 2026-11-28T20:05:09Z | `git log` |
| PR #9 abierto | 2026-11-28T20:09:35Z | `gh pr view` |
| Commit `fix(C-05): fijar UTC en la JVM de tests para pgjdbc` (`d0bcadf`) | 2026-11-28T20:20:03Z | `git log` |
| PR #9 fusionado | 2026-11-28T21:00:54Z | `gh pr view` |

No hay hora registrada del inicio de la exploración, la propuesta ni la implementación. Estos hitos no son intervalos de actividad medidos.

## Integración

| PR | Rama | Merge | Verificación |
|---|---|---|---|
| [#9](https://github.com/Nadia-beltran/turnos-odontologia/pull/9) `C-05-notificaciones-outbox` → `main` | `C-05-notificaciones-outbox` | `372b884` (2026-11-28T21:00:54Z) | `git merge-base --is-ancestor 372b884 origin/main` (OK, 2026-11-28) |

## Limitaciones (por qué es incompleta)

- **Sin contadores de tokens**: el archivo se hizo en otra sesión de Claude Code y no hubo exportador de OpenTelemetry durante la implementación. No hay entrada, salida ni caché por modelo.
- **Sin costo**: no se registró el indicador de costo de la sesión de implementación.
- **Sin intervalos de actividad**: el tiempo sale solo de los hitos de Git y GitHub.
- **Sin validación automática**: no se corrió el validador `usage.py` de `active-delivery`.

## Para las próximas fichas

Configurar `CLAUDE_CODE_ENABLE_TELEMETRY=1` junto con `OTEL_LOGS_EXPORTER` antes de abrir la sesión, registrar la hora UTC al iniciar y pausar cada bloque, y anotar el indicador de costo al cierre.
