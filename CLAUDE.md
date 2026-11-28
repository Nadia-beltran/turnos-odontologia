<!-- active-stack:agent-instruction -->
# Estudio Sonrisa (turnos-odontologia): Instrucciones para Agentes

> Este archivo es lo PRIMERO que todo agente lee al entrar al repo.
> Generado a partir de `knowledge-base/`, `CHANGES.md` y `design-system/` (2026-11-28). Si cambian, volvé a sincronizar este archivo.

SaaS multi-tenant para odontólogos independientes con un sillón. Ataca el **ausentismo y los turnos perdidos** con:

- Reserva online sin cuenta.
- Gestión del turno por email, con cancelación y reprogramación hasta 24 h antes.
- Una **lista de espera** que ofrece automáticamente cada horario liberado.

---

## Stack Tecnológico

| Capa | Tecnología | Versión mínima |
|------|------------|----------------|
| Frontend | TypeScript sin framework + Vite | TS 5.x · Vite 5.x |
| Backend | Spring Boot (Web, Security, Data JPA, Validation, Mail) con Java | 3.x · Java 21 |
| Build | Gradle | 8.x |
| ORM y migraciones | Hibernate (Spring Data JPA) + Flyway | 6.x · 10.x |
| Base de datos | PostgreSQL con la extensión `btree_gist` | 15+ |
| Tests | JUnit + Testcontainers (PostgreSQL real) | — |
| Email | Proveedor SMTP o API gratuito (outbox) | Pendiente: PA-03 |
| Hosting | Planes gratuitos; el frontend estático reenvía `/api/*` al backend | Pendiente: PA-03 |

Detalle completo: [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md)

---

## Base de Conocimiento

La fuente de verdad del dominio vive en `knowledge-base/`. **Leé el archivo relevante ANTES de implementar.**

| Archivo | Cuándo leerlo |
|---------|---------------|
| [01_vision_y_objetivos.md](knowledge-base/01_vision_y_objetivos.md) | Propósito, alcance v1 y métricas |
| [03_actores_y_roles.md](knowledge-base/03_actores_y_roles.md) | Roles, permisos, rutas públicas |
| [04_modelo_de_datos.md](knowledge-base/04_modelo_de_datos.md) | Entidades, restricción de exclusión, seed |
| [05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Reglas codificadas RN-XX-NN |
| [06_funcionalidades.md](knowledge-base/06_funcionalidades.md) | Historias US-NNN por épica |
| [07_flujos_principales.md](knowledge-base/07_flujos_principales.md) | Flujos de punta a punta (reserva, gestión, lista de espera, outbox) |
| [08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Módulos, multi-tenancy, seguridad, tests, variables de entorno |
| [09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) | Decisiones DD-NN y supuestos SU-NN |
| [10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) | ⚠️ Inconsistencias y preguntas que hay que resolver ANTES de codificar |
| [11_notificaciones_email.md](knowledge-base/11_notificaciones_email.md) | Emails, cuota diaria, entregabilidad |
| [12_despliegue_y_entornos.md](knowledge-base/12_despliegue_y_entornos.md) | Entornos, hosting gratuito, *keep-alive*, CI |

> ⚠️ Preguntas de prioridad **Alta** abiertas: PA-03 (hosting y email, se resuelve en C-03), PA-07 (revisión legal), PA-08 (dominio propio) y PA-01 (límites del plan gratuito). Ninguna bloquea C-01 ni C-02.

**Design system**: [design-system/README.md](design-system/README.md). Revisión 1 aprobada, política `system-and-mockups`.

---

## Skills Disponibles

| Agente | Rol | Skills que carga |
|--------|-----|------------------|
| **Backend Core** | Spring Boot, JPA, Flyway, tenancy, turnos | `openspec-apply-change`, `active-delivery` |
| **Backend Aux** | Outbox de emails, lista de espera, reportes, despliegue | `openspec-apply-change`, `active-delivery` |
| **Frontend** | TS sin framework, páginas del paciente, panel | `design-system`, `emil-design-eng`, `design-taste-frontend-v1` (parcial: gana `design-system/`), `openspec-apply-change` |
| **Planificación** | Proponer, explorar y archivar changes | `openspec`, `openspec-propose`, `openspec-explore`, `openspec-update-change`, `openspec-sync-specs`, `openspec-archive-change` |
| **Fundación** | Contexto e instrucciones | `active-orchestrator`, `kb-creator`, `roadmap-generator`, `agent-instruction`, `skill-registry` |

Cargá la skill correspondiente al contexto ANTES de escribir código. No hay skills instaladas específicas de Spring Boot ni de PostgreSQL. `design-mockups` (mockups por change) **no está instalada**: hasta que se instale, los mockups siguen `design-system/README.md`.

> Las compact rules de cada skill las resuelve el orquestador desde [.atl/skill-registry.md](.atl/skill-registry.md) (generado por `skill-registry`; no versionado, no está en el repo). Esta tabla solo mapea skill → rol.

---

## Roadmap de Changes

El plan de implementación completo está en [CHANGES.md](CHANGES.md). Resumen:

- **Total**: 24 changes en 9 fases, cada uno con responsable asignado (m1 a m5, ver `.active-stack/team.json`).
- **Camino crítico** (12): `C-01 → C-04 → C-05 → C-06 → C-08 → C-10 → C-11 → C-12 → C-14 → (C-17 | C-16) → C-18 → C-24`.
- **Primeros changes**: `C-01` (`backend-scaffolding`) y luego `C-02` (`frontend-scaffolding`). Después, `C-03` despliega temprano para validar el hosting gratuito.
- **Changes con pantallas** (C-07, C-09, C-13, C-16, C-18, C-20, C-22): requieren un mockup aprobado antes de implementarse.

**Antes de iniciar un item**: leé [CHANGES.md](CHANGES.md), verificá que sus dependencias estén `[x]` y leé los archivos de "Leer antes".

---

## Reglas Duras (específicas del proyecto)

> Reglas globales ya definidas en el archivo global de instrucciones del cliente activo (OpenSpec, governance por dominio, Engram, mockups, diagramas): el proyecto las hereda. Acá viven solo las reglas **específicas de este proyecto** y las universales que el global no cubre. Son contrato: romperlas es un defecto.

**Backend**
- NUNCA consultar ni insertar datos de negocio sin el filtro de consultorio, ni aceptar un `consultorio_id` enviado por el cliente → usar `TenantContext` y el filtro de Hibernate (RN-MT-01, RN-MT-02).
- NUNCA cambiar el esquema con `ddl-auto` ni editar una migración de Flyway ya fusionada → crear una migración nueva con el número reservado en `CHANGES.md`.
- NUNCA probar la persistencia con H2 ni con mocks de la base → usar PostgreSQL real con Testcontainers (la restricción de exclusión solo existe ahí).
- NUNCA hacer UPDATE ni DELETE sobre `odontograma_registro`, `evolucion_clinica` o `auditoria` → append-only: una corrección es un registro nuevo (RN-CL-01).
- NUNCA guardar tokens en claro, ni poner DNI o datos clínicos en logs, emails o URLs → guardar solo el hash SHA-256 y registrar solo identificadores (RN-SG-01, RN-SG-06).
- NUNCA enviar un email directamente desde un servicio → encolarlo en `notificacion` (outbox) dentro de la misma transacción (RN-NO-01).
- NUNCA exponer entidades JPA en los controllers → usar DTOs con Bean Validation, inyección por constructor y errores como `ProblemDetail`.
- NUNCA usar la hora local del servidor → guardar en UTC (`timestamptz`) y mostrar en la zona horaria del consultorio (RN-EX-02).

**Frontend**
- NUNCA usar `any` ni `innerHTML` con datos → `tsconfig` estricto y `textContent` o los helpers de `src/shared/dom.ts`.
- NUNCA agregar frameworks de UI (React o similares), fuentes remotas ni CDN → TypeScript sin framework y recursos locales.
- NUNCA escribir colores, tamaños o fuentes a mano → usar los tokens de `design-system/tokens.css`. NUNCA implementar una pantalla sin su mockup aprobado.

**Git**
- Commits con Conventional Commits que referencian la ficha: `feat(C-05): outbox de emails`, `fix(C-12): …`.
- NUNCA agregar líneas de co-autoría de IA (`Co-Authored-By`) en commits ni en PR.

---

## Flujo de Trabajo

```
1. Identificar tu ficha en CHANGES.md (Responsable)  → respetar dependencias
2. Leer la KB indicada en "Leer antes"               → entender el dominio
3. Acordar alcance y criterios (active-delivery: ODD o SDD con OpenSpec)
4. Si toca pantallas: mockup aprobado antes de implementar
5. Implementar y verificar (tests críticos de 08 §Calidad y tests)
6. PR con la plantilla; revisa el responsable de la siguiente ficha (y CODEOWNERS en zonas de riesgo)
7. Al fusionar: marcar [x] en CHANGES.md y dejar evidencia en el PR
```

Aplicá TODAS las reglas duras en cada paso. Ante un conflicto entre la KB y este archivo, prevalecen las reglas duras.
<!-- /active-stack:agent-instruction -->

<!-- active-stack:team-mode -->
## Reglas de equipo

Proyecto de equipo: la configuración vive en `.active-stack/team.json`.

- Al iniciar la sesión, si existe `.active-stack/team.json`, usá la skill `active-delivery` para identificar a la persona y mostrar «qué me toca». Detalle en las `references/` de esa skill.
- Una rama y un worktree por ficha: `<id>-slug`. Nunca dos agentes en la misma rama o carpeta.
- Nada directo a `main`. PR chicos. Commit, push y PR solo con autorización de la persona.
- No inventes nombres ni cuentas de integrantes: lo que falte queda pendiente hasta que el equipo lo complete.

**Si la skill `active-delivery` no está disponible en este cliente**, aplicá la misma convención a mano:

1. Una rama por ficha (`<id>-slug`) creada desde la rama principal actualizada.
2. Todo cambio entra por pull request; sin push a `main` ni force-push.
3. Al terminar, dejá el handoff en `knowledge-base/entrega/<id>.md`: qué se hizo, cómo se verificó, qué sigue y qué decisiones quedan abiertas.
<!-- /active-stack:team-mode -->
