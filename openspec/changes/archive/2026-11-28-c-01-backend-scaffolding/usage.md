# Evidencia de uso: C-01 backend-scaffolding

**Estado de la evidencia: incompleta, aceptada por la usuaria (Nadia, m1) el 2026-11-28 para archivar el change.**

## Costo y tiempo reportados

| Dato | Valor | Fuente |
|---|---|---|
| Costo acumulado de la sesión | USD 3,78 | Indicador de costo del cliente Claude Code, informado por la usuaria |
| Costo del último turno | USD 0,54 | Ídem |
| Duración del último turno | 4 min 40 s | Ídem |
| Modelo | claude-opus-5-5 | Modelo de la sesión |
| Bloque de trabajo registrado | 2026-11-28T15:00:21Z → 15:32:29Z | Hora UTC tomada al iniciar y pausar la implementación |

## Integración

| PR | Merge | Verificación |
|---|---|---|
| #1 `C-01-backend-scaffolding` | `71ccc90` | `git merge-base --is-ancestor 71ccc90 origin/main` |
| #2 `C-01-cierre` | `fd56632` | `git merge-base --is-ancestor fd56632 origin/main` |

## Limitaciones (por qué es incompleta)

- **Sin desglose de tokens**: la telemetría (`CLAUDE_CODE_ENABLE_TELEMETRY`) estuvo apagada. No hay contadores de entrada, salida ni caché por modelo; solo la cifra agregada del cliente.
- **Sesión compartida**: el costo de USD 3,78 corresponde a toda la sesión. Incluye la fundación del proyecto (discovery, KB, design system, roadmap, equipo), no solo C-01.
- **Intervalos parciales**: solo se registró un bloque. Las sesiones de propuesta, CI y cierre no tienen horas de inicio y fin.
- **Sin validación automática**: el validador de uso de `active-delivery` requiere Python 3.8+, que no está instalado.

## Para las próximas fichas

Activar `CLAUDE_CODE_ENABLE_TELEMETRY=1` antes de abrir la sesión de trabajo y registrar el inicio y fin de cada bloque, para obtener evidencia completa y validable.
