# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **Estudio Sonrisa** (turnos-odontologia).
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**

---

## Cómo usar este documento

1. Elegí el item a implementar y verificá que sus dependencias figuren completas en este roadmap.
2. Leé los docs de la knowledge-base indicados en "Leer antes".
3. Acordá el alcance y los criterios de aceptación del item. **Si el item cambia pantallas, necesita un mockup aprobado antes de implementar** (política `system-and-mockups`, `design-system/README.md`, revisión 1).
4. Implementá, verificá los criterios acordados y registrá la evidencia.
5. Marcá el checkbox `[x]` en este archivo al completar la verificación técnica.

Conservá los IDs de las fichas al editar o reordenar. El seguimiento de tareas es **local** (este archivo y `.active-stack/roadmap.json`).

## Revisión de PR en equipo

En modo equipo, al abrir un PR solicitá revisión al responsable de la siguiente ficha elegible según el orden de este roadmap. Saltá las fichas con cierre u omisión confirmados. Si no hay ficha siguiente, o su responsable no está confirmado, es el autor o no está disponible, el equipo aprueba un revisor independiente y registra su identidad en el PR.

El revisor comprueba el SHA actual y los checks requeridos. Si todo está correcto, aprueba y fusiona el PR tras cumplir las revisiones y checks exigidos. Si encuentra errores, solicita cambios y devuelve el PR abierto al autor. El autor actualiza la misma rama y pide otra revisión del nuevo SHA.

---

## Árbol de dependencias

```
C-01 backend-scaffolding
  ├── C-03 despliegue-esqueleto                ← + C-02 (valida hosting gratuito temprano)
  └── C-04 tenancy-y-auditoria-base
        ├── C-05 notificaciones-outbox
        │     └── C-06 auth-cuentas-sesion     ← desbloquea todo el dominio
        │           ├── C-07 auth-ui-panel-shell            ← + C-02
        │           └── C-08 configuracion-consultorio-api
        │                 ├── C-09 configuracion-consultorio-ui   ← + C-07
        │                 └── C-10 pacientes-turnos-modelo
        │                       ├── C-11 disponibilidad
        │                       │     └── C-12 turnos-consultorio-api   ← + C-05
        │                       │           ├── C-13 agenda-pacientes-ui      ← + C-09
        │                       │           ├── C-14 reserva-online-api
        │                       │           │     ├── C-17 lista-espera-api
        │                       │           │     └── C-23 admin-saas         ← + C-07
        │                       │           └── C-15 gestion-turno-y-recordatorios-api
        │                       │                 └── C-16 paginas-paciente-ui   ← + C-14
        │                       │                       └── C-18 lista-espera-ui ← + C-17, C-13
        │                       └── C-19 ficha-clinica-api
        │                             └── C-20 ficha-clinica-ui          ← + C-13
        └── (C-21 reportes-auditoria-exportacion-api ← C-12, C-17, C-19)
                  └── C-22 reportes-auditoria-ui                         ← + C-13

C-02 frontend-scaffolding    ← paralelo con C-01 desde el inicio

C-24 lanzamiento-produccion  ← C-03, C-16, C-18, C-20, C-23   (C-21/C-22 no bloquean el lanzamiento)
```

### Paralelismo por fase

```
GATE 0: inicio
  → C-01 backend-scaffolding                  [Agente A]
  → C-02 frontend-scaffolding                 [Agente C]

GATE 1: C-01 ✓
  → C-04 tenancy-y-auditoria-base             [Agente A]
  → C-03 despliegue-esqueleto                 [Agente B — si C-02 ✓]

GATE 2: C-04 ✓
  → C-05 notificaciones-outbox                [Agente A]

GATE 3: C-05 ✓
  → C-06 auth-cuentas-sesion                  [Agente A]

GATE 4: C-06 ✓
  → C-08 configuracion-consultorio-api        [Agente A]
  → C-07 auth-ui-panel-shell                  [Agente C]

GATE 5: C-08 ✓
  → C-10 pacientes-turnos-modelo              [Agente A]
  → C-09 configuracion-consultorio-ui         [Agente C — si C-07 ✓]

GATE 6: C-10 ✓
  → C-11 disponibilidad                       [Agente A]
  → C-19 ficha-clinica-api                    [Agente B]

GATE 7: C-11 ✓
  → C-12 turnos-consultorio-api               [Agente A]

GATE 8: C-12 ✓                                 ← FORK
  → C-14 reserva-online-api                   [Agente A]
  → C-15 gestion-turno-y-recordatorios-api    [Agente B]
  → C-13 agenda-pacientes-ui                  [Agente C — si C-09 ✓]

GATE 9: C-14 ✓                                 ← FORK
  → C-17 lista-espera-api                     [Agente A]
  → C-23 admin-saas                           [Agente B — si C-07 ✓]
  → C-16 paginas-paciente-ui                  [Agente C — si C-15 ✓]

GATE 10: C-17 ✓                                ← FORK
  → C-21 reportes-auditoria-exportacion-api   [Agente A — si C-19 ✓]
  → C-20 ficha-clinica-ui                     [Agente B — si C-13, C-19 ✓]
  → C-18 lista-espera-ui                      [Agente C — si C-16, C-13 ✓]

GATE 11: C-21 ✓
  → C-22 reportes-auditoria-ui                [Agente B — si C-13 ✓]

GATE 12: C-03, C-16, C-18, C-20, C-23 ✓
  → C-24 lanzamiento-produccion               [Agentes A + C; B termina C-22 en paralelo]
```

### Camino crítico (12 changes — mínimo irreducible)

```
C-01 → C-04 → C-05 → C-06 → C-08 → C-10 → C-11 → C-12 → C-14 → (C-17 | C-16)* → C-18 → C-24
```

\* C-17 (lista de espera API) y C-16 (páginas del paciente) tienen el mismo largo y ambos bloquean C-18: van en paralelo. Los reportes (C-21, C-22) quedan fuera del camino crítico: la línea base de ausentismo se toma desde el registro de asistencia (C-12/C-13), y la pantalla de reportes puede salir junto con el lanzamiento o justo después.

### Plan óptimo con 3 agentes

Los mockups (`design-mockups`) son prerrequisito de cada change visual: el Agente C los adelanta mientras el backend avanza.

| Paso | Agente A (Backend Core) | Agente B (Backend Aux / Infra) | Agente C (Frontend) |
|---|---|---|---|
| 1 | C-01 backend-scaffolding | — | C-02 frontend-scaffolding |
| 2 | C-04 tenancy-y-auditoria-base | C-03 despliegue-esqueleto | Mockups de C-07 y C-09 |
| 3 | C-05 notificaciones-outbox | C-03 (cierre: keep-alive, `btree_gist`) | Mockups de C-13 y C-16 |
| 4 | C-06 auth-cuentas-sesion | Revisión de C-05 y C-06 | Mockups de C-18, C-20 y C-22 |
| 5 | C-08 configuracion-consultorio-api | — | C-07 auth-ui-panel-shell |
| 6 | C-10 pacientes-turnos-modelo | — | C-09 configuracion-consultorio-ui |
| 7 | C-11 disponibilidad | C-19 ficha-clinica-api | — |
| 8 | C-12 turnos-consultorio-api | C-19 (cierre) | — |
| 9 | C-14 reserva-online-api | C-15 gestion-turno-y-recordatorios-api | C-13 agenda-pacientes-ui |
| 10 | C-17 lista-espera-api | C-23 admin-saas | C-16 paginas-paciente-ui |
| 11 | C-21 reportes-auditoria-exportacion-api | C-20 ficha-clinica-ui | C-18 lista-espera-ui |
| 12 | C-24 lanzamiento-produccion | C-22 reportes-auditoria-ui | C-24 lanzamiento-produccion |

> **Equipo de 5 personas**: el reparto real por persona lo propone la fase `team-mode` (`fichas verify` / `split`). Las 2 personas extra rinden más como revisores de los changes CRITICO y ALTO, y adelantando mockups.

---

## FASE 1 — Fundación técnica

> C-01 y C-02 arrancan en paralelo. C-03 valida el hosting gratuito lo antes posible (PA-03, de prioridad alta).

### [C-01] `backend-scaffolding`
- **Estado**: `[ ]` pendiente
- **Responsable**: m1
- **Resultado**: `./gradlew build` pasa en local y en CI, y un test de integración levanta PostgreSQL con Testcontainers.
- **Zona**: `backend/`, `docker-compose.yml`, `.github/workflows/backend.yml`
- **Scope**:
  - Proyecto Gradle con Spring Boot 4.2.0, Java 17, Web, Security, Data JPA, Validation, Mail y Flyway (DD-15).
  - Paquetes por módulo según `08` (`ar.turnos.{shared,auth,consultorio,agenda,turnos,listaespera,pacientes,clinica,notificaciones,auditoria,reportes,admin}`).
  - Perfiles `dev` y `prod`, y `application.yml` con las variables de `08 §Variables de entorno`.
  - `docker-compose.yml`: PostgreSQL 15 y Mailpit.
  - Migración `V1__extensiones.sql`: `CREATE EXTENSION btree_gist`.
  - Manejo global de errores con `ProblemDetail` (RFC 9457).
  - Base de Testcontainers reutilizable.
  - CI en GitHub Actions: build y tests.
- **Dependencias**: ninguna
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/02_descripcion_general.md` §Stack tecnológico
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios, §Variables de entorno
  - `knowledge-base/12_despliegue_y_entornos.md` §Entornos, §Integración continua
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-13, SU-01

### [C-02] `frontend-scaffolding`
- **Estado**: `[ ]` pendiente
- **Responsable**: m1
- **Resultado**: `npm run build` compila sin errores de tipos, y `vite dev` sirve un layout base con `tokens.css`.
- **Zona**: `frontend/`, `.github/workflows/frontend.yml`
- **Scope**:
  - Vite con TypeScript sin framework y `tsc --noEmit` en el build.
  - `src/router.ts` (History API) y `src/shared/api.ts`: `fetch` con `credentials: 'same-origin'`, header `X-XSRF-TOKEN` y parseo de `ProblemDetail`.
  - `src/shared/dom.ts`: helpers que usan `textContent`, nunca `innerHTML` con datos.
  - Import de `design-system/tokens.css` y layouts base: público (celular primero) y panel.
  - Proxy de `vite dev` de `/api` a `localhost:8080`.
  - `public/_redirects` (o el equivalente del hosting): `/api/*` → backend, `/*` → `index.html`.
  - CI: `npm ci && npm run build`.
- **Dependencias**: ninguna
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios, §Seguridad
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-02, DD-03
  - `design-system/README.md`
  - `knowledge-base/12_despliegue_y_entornos.md` §Requisitos del hosting

### [C-03] `despliegue-esqueleto`
- **Estado**: `[ ]` pendiente
- **Responsable**: m1
- **Resultado**: en producción, el frontend responde, `/api/health` llega al backend a través del reenvío, la cookie de prueba es de primera parte, la migración con `btree_gist` corre en la base gratuita y un cron externo llama a `/api/internal/tick`.
- **Zona**: `frontend/public/_redirects`, `backend/src/main/java/ar/turnos/shared/`, `docs/despliegue.md`, `knowledge-base/09_decisiones_y_supuestos.md`
- **Scope**:
  - Elegir el hosting del frontend, el backend y PostgreSQL según los criterios de `12` (resuelve **PA-03**) y registrar la decisión como una DD nueva.
  - Endpoints `GET /api/health` y `POST /api/internal/tick` (header `X-Tick-Secret`; en este change, sin lógica).
  - Despliegue del backend y del frontend con el reenvío de `/api`.
  - Variables de entorno de producción.
  - Cron externo gratuito cada 5 a 10 minutos.
  - Prueba manual: cookie `SameSite=Lax` en el mismo origen.
  - `docs/despliegue.md` con los pasos reproducibles.
- **Dependencias**: `C-01, C-02`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/12_despliegue_y_entornos.md`
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-01, DD-03, DD-04
  - `knowledge-base/10_preguntas_abiertas.md` PA-03, PA-13
  - `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno

### [C-04] `tenancy-y-auditoria-base`
- **Estado**: `[ ]` pendiente
- **Responsable**: m2
- **Resultado**: los tests de integración prueban que una entidad de un consultorio nunca es visible ni modificable desde otro, y que las acciones quedan en `auditoria`.
- **Zona**: `backend/src/main/java/ar/turnos/shared/`, `backend/src/main/java/ar/turnos/auditoria/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V2__tenancy.sql`: tablas `consultorio`, `usuario` (con CHECK de rol y consultorio) y `sillon`.
  - Migración `V3__auditoria.sql`: tabla `auditoria` (append-only).
  - `BaseTenantEntity` con `consultorio_id`, `@FilterDef("tenant")` y `TenantContext`.
  - Filtro o interceptor que habilita el filtro de Hibernate por request; los inserts toman el `consultorio_id` del contexto.
  - `AuditService.registrar(accion, entidad, id, datos)`.
  - Suite de aislamiento reutilizable con dos consultorios, para extender en cada change.
- **Dependencias**: `C-01`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Convenciones, §consultorio, §usuario, §sillon, §auditoria
  - `knowledge-base/05_reglas_de_negocio.md` §RN-MT, §RN-SG-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Multi-tenancy (implementación)
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-01, DD-05

---

## FASE 2 — Notificaciones y acceso

> Auth depende de la cola de emails (verificación y recuperación). El panel del frontend arranca apenas existe la sesión.

### [C-05] `notificaciones-outbox`
- **Estado**: `[ ]` pendiente
- **Responsable**: m5
- **Resultado**: una notificación encolada en una transacción llega a Mailpit, respetando prioridad, reintentos y cuota diaria; `tick` la procesa igual que la tarea programada.
- **Zona**: `backend/src/main/java/ar/turnos/notificaciones/`, `backend/src/main/resources/templates/email/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V4__notificacion.sql`, con índice (`estado`, `programada_para`).
  - `NotificacionService.encolar(tipo, destinatario, payload, programadaPara)` (patrón outbox).
  - `@Scheduled` cada 5 minutos y `POST /api/internal/tick`, que comparten el procesador. Selección con `FOR UPDATE SKIP LOCKED`.
  - Prioridades 1 a 4, `APP_MAIL_DAILY_QUOTA` (umbral del 80 %) y reintentos a los 5, 15 y 60 minutos, hasta quedar `FALLIDA`.
  - Plantillas HTML y de texto plano con remitente "{consultorio} vía {BRAND_NAME}" (constante de configuración, DD-14).
  - Tests: envío, reintento, cuota agotada y prioridad.
- **Dependencias**: `C-04`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/11_notificaciones_email.md`
  - `knowledge-base/07_flujos_principales.md` §Flujo 6
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-09, DD-14

### [C-06] `auth-cuentas-sesion`
- **Estado**: `[ ]` pendiente
- **Responsable**: m2
- **Resultado**: un odontólogo se registra, verifica su email, inicia y cierra sesión y recupera su contraseña. El alta crea el consultorio completo, y la sesión sobrevive a un reinicio del backend.
- **Zona**: `backend/src/main/java/ar/turnos/auth/`, `backend/src/main/java/ar/turnos/consultorio/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - `POST /api/auth/registro`, `/login`, `/logout`, `/verificar-email`, `/recuperar` y `/restablecer`; `GET /api/auth/me` (US-001 a US-004).
  - Alta transaccional: consultorio `PENDIENTE_VERIFICACION`, usuario `ODONTOLOGO`, sillón, prestaciones sugeridas y horario por defecto (RN-AU-01).
  - Spring Security con sesión: cookie `HttpOnly`, `Secure` y `SameSite=Lax`, `CookieCsrfTokenRepository` y Spring Session JDBC (migración `V5__spring_session.sql`).
  - BCrypt, bloqueo tras 5 intentos fallidos, vencimiento de 8 h y límite de tasa en `/api/auth/**`.
  - Tokens de verificación y recuperación guardados como hash.
  - Seed del `ADMIN_SAAS` desde variables de entorno.
  - `@PreAuthorize` por rol.
  - Tests: registro, verificación, bloqueo, CSRF y aislamiento.
- **Dependencias**: `C-04, C-05`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md`
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AU, §RN-SG
  - `knowledge-base/07_flujos_principales.md` §Flujo 1
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-11, SU-06

### [C-07] `auth-ui-panel-shell`
- **Estado**: `[ ]` pendiente
- **Responsable**: m3
- **Resultado**: las páginas de registro, login, verificación y recuperación funcionan contra la API, y el panel protegido redirige al login sin sesión. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/auth/`, `frontend/src/pages/panel/`, `frontend/src/shared/`
- **Scope**:
  - **Requiere un mockup aprobado** (`openspec/changes/<change>/mockups/`) antes de implementar.
  - Páginas `/registro`, `/login`, `/verificar-email` y `/recuperar`.
  - Shell del panel: navegación (Agenda, Pacientes, Lista de espera, Reportes, Configuración), usuario actual, logout y guard de ruta con `GET /api/auth/me`.
  - Manejo de 401 y 403 en `api.ts`.
  - Estados de error y de carga según `design-system/components.html`.
- **Dependencias**: `C-02, C-06`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 1
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas
  - `design-system/README.md`
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad

---

## FASE 3 — Configuración del consultorio

### [C-08] `configuracion-consultorio-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m5
- **Resultado**: el odontólogo configura datos, slug, horarios, prestaciones, bloqueos y reglas de reserva y recordatorio, con validaciones y auditoría.
- **Zona**: `backend/src/main/java/ar/turnos/consultorio/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V6__configuracion.sql`: `prestacion`, `horario_atencion`, `bloqueo`, y columnas de reglas en `consultorio`.
  - `GET/PUT /api/consultorio`, validando el slug (RN-AU-07).
  - `GET/PUT /api/consultorio/horarios`, sin franjas superpuestas.
  - `CRUD /api/prestaciones`: duración de 5 a 480 minutos en múltiplos de 5, `reservable_online`.
  - `GET/POST/DELETE /api/bloqueos`: devuelve los turnos afectados, sin cancelarlos (RN-AG-06).
  - Reglas: anticipación mínima, horizonte y `horas_recordatorio` de 25 h o más (US-014).
  - Tests: validaciones y aislamiento.
- **Dependencias**: `C-06`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §consultorio, §prestacion, §horario_atencion, §bloqueo
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AG, §RN-AU-07, §RN-NO-02
  - `knowledge-base/06_funcionalidades.md` §Épica 2
  - `knowledge-base/02_descripcion_general.md` §API REST

### [C-09] `configuracion-consultorio-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m3
- **Resultado**: las pantallas de configuración permiten editar todo lo de C-08 y copiar el link público. Coinciden con el mockup aprobado.
- **Zona**: `frontend/src/pages/panel/configuracion/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - Secciones Datos y link, Horarios (franjas por día), Prestaciones (tabla y formulario), Bloqueos y feriados (aviso de turnos afectados) y Reglas de reserva y recordatorio.
- **Dependencias**: `C-07, C-08`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 2
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AG-06, §RN-NO-02
  - `design-system/components.html`
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios

---

## FASE 4 — Núcleo de turnos

> C-10 y C-11 son la columna vertebral: casi todo lo que sigue depende de ellos.

### [C-10] `pacientes-turnos-modelo`
- **Estado**: `[ ]` pendiente
- **Responsable**: m2
- **Resultado**: la restricción de exclusión impide dos turnos activos superpuestos en el mismo sillón y permite sobreturnos. La máquina de estados rechaza las transiciones inválidas. Probado con Testcontainers, incluyendo inserts concurrentes.
- **Zona**: `backend/src/main/java/ar/turnos/turnos/`, `backend/src/main/java/ar/turnos/pacientes/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V7__pacientes_turnos.sql`: tablas `paciente` (UNIQUE `consultorio_id` + `dni`), `turno` con `EXCLUDE USING gist` (RN-AG-01) y `token_gestion`.
  - Entidades con `@Version`.
  - `TurnoEstado` y las transiciones de RN-TU-01 en el dominio.
  - Normalización de DNI (RN-PA-02).
  - `TokenService` (128 bits o más, hash SHA-256, vencimiento y revocación).
  - Evento de dominio `TurnoLiberado`.
  - Tests: exclusión (con y sin sobreturno), concurrencia y transiciones.
- **Dependencias**: `C-08`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §paciente, §turno, §token_gestion
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AG-01, §RN-TU, §RN-PA, §RN-SG-01
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-04
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones aplicados, §Calidad y tests

### [C-11] `disponibilidad`
- **Estado**: `[ ]` pendiente
- **Responsable**: m5
- **Resultado**: `DisponibilidadService` devuelve los horarios correctos para cualquier combinación de franjas, bloqueos, turnos, anticipación, horizonte y zona horaria, cubierto por tests unitarios de borde.
- **Zona**: `backend/src/main/java/ar/turnos/agenda/`
- **Scope**:
  - `DisponibilidadService.calcular(consultorio, prestacion, desde, hasta)`.
  - Reglas RN-AG-04 (horario, bloqueo, turnos activos, anticipación, horizonte, que entre completo) y RN-AG-05 (pasos de 15 minutos).
  - Cálculo en la zona horaria del consultorio; se guarda en UTC (RN-EX-02).
  - Tests: franjas partidas, bloqueo parcial, fin de franja, cambio de día y turnos cancelados que liberan horario.
- **Dependencias**: `C-10`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AG, §RN-EX-02
  - `knowledge-base/07_flujos_principales.md` §Flujo 2 (paso 3)
  - `knowledge-base/04_modelo_de_datos.md` §horario_atencion, §bloqueo, §turno
  - `knowledge-base/09_decisiones_y_supuestos.md` SU-10

### [C-12] `turnos-consultorio-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m2
- **Resultado**: el consultorio carga, cancela, reprograma y registra asistencia de turnos, y busca y edita pacientes. Cada cancelación o reprogramación publica `TurnoLiberado` después del commit y encola el email.
- **Zona**: `backend/src/main/java/ar/turnos/turnos/`, `backend/src/main/java/ar/turnos/pacientes/`
- **Scope**:
  - `GET /api/agenda?desde=&hasta=`, con pendientes de registrar (RN-AS-01).
  - `POST /api/turnos`: manual, con opción de sobreturno (RN-AG-02 y RN-AG-07). Un 409 ofrece sobreturno.
  - `POST /api/turnos/{id}/cancelar` y `/reprogramar`: atómicos (RN-TU-04 a RN-TU-07).
  - `PATCH /api/turnos/{id}/estado`: `ATENDIDO` o `AUSENTE`, solo después del inicio, con corrección auditada.
  - `GET /api/pacientes?q=` y `GET/PUT /api/pacientes/{id}`, con contador de ausencias.
  - Emails `RESERVA_CONFIRMADA`, `TURNO_CANCELADO` y `TURNO_REPROGRAMADO` mediante el outbox.
  - Tests: reprogramación atómica, asistencia y aislamiento.
- **Dependencias**: `C-05, C-10, C-11`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 3, §US-060
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU, §RN-AS
  - `knowledge-base/07_flujos_principales.md` §Flujo 5
  - `knowledge-base/04_modelo_de_datos.md` §turno

### [C-13] `agenda-pacientes-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m3
- **Resultado**: agenda diaria y semanal usable en escritorio y en el celular, con carga y gestión de turnos, registro de asistencia y listado de pacientes. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/panel/agenda/`, `frontend/src/pages/panel/pacientes/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - Vistas Día y Semana, con estados, sobreturnos, bloqueos y pendientes de registrar (`design-system`).
  - Modal de nuevo turno: búsqueda por DNI o apellido, o alta de paciente; ante un 409, ofrecer sobreturno.
  - Acciones cancelar, reprogramar, Atendido y Ausente.
  - Listado de pacientes y edición de contacto.
- **Dependencias**: `C-09, C-12`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 3, §US-060
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-01, §RN-AS-01
  - `design-system/components.html` §Panel del consultorio
  - `knowledge-base/07_flujos_principales.md` §Flujo 5

---

## FASE 5 — Paciente: reserva y gestión del turno

> C-14 y C-15 van en paralelo tras C-12.

### [C-14] `reserva-online-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m5
- **Resultado**: un paciente sin cuenta reserva por slug. Se respetan las reglas de DNI y apellido, el límite de 2 turnos, el límite del plan y el límite de tasa. Una carrera por el mismo horario devuelve 409 con alternativas.
- **Zona**: `backend/src/main/java/ar/turnos/turnos/publico/`, `backend/src/main/java/ar/turnos/shared/ratelimit/`
- **Scope**:
  - `GET /api/public/c/{slug}`, `/prestaciones` y `/disponibilidad`.
  - `POST /api/public/c/{slug}/reservas` (RN-RE-01 a RN-RE-05).
  - Resolución del consultorio solo por slug (RN-MT-02); se rechaza si está suspendido o no verificado.
  - Límite del plan gratuito, configurable (RN-PL-01; los valores dependen de PA-01).
  - Bucket4j en `/api/public/**` (RN-SG-05).
  - Emails `RESERVA_CONFIRMADA` y `RECORDATORIO` programado.
  - Tests: carrera concurrente, apellido que no coincide, límite por DNI y 429.
- **Dependencias**: `C-11, C-12`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §RN-RE, §RN-MT-02, §RN-PL, §RN-SG-05
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/06_funcionalidades.md` §Épica 4
  - `knowledge-base/09_decisiones_y_supuestos.md` SU-07, SU-08

### [C-15] `gestion-turno-y-recordatorios-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m4
- **Resultado**: con el link del email, el paciente confirma (idempotente), cancela o reprograma hasta 24 h antes. Después de ese plazo, la API devuelve 409 con el contacto. Los recordatorios salen `horas_recordatorio` antes y no se envían para turnos inactivos.
- **Zona**: `backend/src/main/java/ar/turnos/turnos/gestion/`, `backend/src/main/java/ar/turnos/notificaciones/`
- **Scope**:
  - `GET /api/public/turnos/{token}`: datos sin información clínica y acciones permitidas (RN-SG-06).
  - `POST .../confirmar`, `.../cancelar` y `.../reprogramar` (RN-TU-03, RN-TU-05 a RN-TU-07, RN-EX-01).
  - Rotación de token al reprogramar.
  - Programación y descarte de recordatorios (RN-NO-02, DD-12).
  - Tests: borde de 24 h con zona horaria, idempotencia, token revocado o vencido.
- **Dependencias**: `C-12`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 3
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-03, §RN-NO-02, §RN-SG-01, §RN-SG-06
  - `knowledge-base/06_funcionalidades.md` §Épica 5
  - `knowledge-base/11_notificaciones_email.md`

### [C-16] `paginas-paciente-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m4
- **Resultado**: en un celular de 390 px el paciente completa la reserva en 3 pasos y gestiona su turno desde el link. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/publico/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - `/c/{slug}`: prestación, día y horario, datos, confirmación. Ante un 409 ofrece alternativas; si no hay horarios, enlaza a la lista de espera.
  - `/t/{token}`: confirmar, cancelar (con confirmación), reprogramar, y la variante de menos de 24 h con contacto.
  - Pantalla de link no válido.
  - `Referrer-Policy: no-referrer`.
  - Verificación manual en el celular.
- **Dependencias**: `C-02, C-14, C-15`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 4, §Épica 5
  - `knowledge-base/07_flujos_principales.md` §Flujo 2, §Flujo 3
  - `design-system/components.html` §Páginas del paciente
  - `design-system/brand.md` §Tono de voz

---

## FASE 6 — Lista de espera (diferencial)

### [C-17] `lista-espera-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m2
- **Resultado**: al liberarse un turno se ofrece a los inscriptos compatibles (como máximo 20). Con dos aceptaciones simultáneas gana exactamente una y la otra recibe 409. Las ofertas vencen y las inscripciones caducan.
- **Zona**: `backend/src/main/java/ar/turnos/listaespera/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V8__lista_espera.sql`: `inscripcion_espera` (UNIQUE parcial ACTIVA), `oferta_hueco` (`@Version`) y `oferta_envio`.
  - `POST /api/public/c/{slug}/lista-espera` y baja por token (RN-LE-01, RN-LE-08).
  - `@TransactionalEventListener(AFTER_COMMIT)` sobre `TurnoLiberado`: crea la oferta y los envíos según RN-LE-02 (tope SU-14).
  - `GET /api/public/ofertas/{token}` y `POST .../aceptar` (RN-LE-03 a RN-LE-06): 409 si ya fue tomada, 410 si expiró.
  - Expiración de ofertas y vencimiento de inscripciones en el `tick` (RN-LE-05, RN-LE-07).
  - `GET /api/lista-espera` y `DELETE /api/lista-espera/{id}` para el panel.
  - Tests: aceptación concurrente con dos hilos, horario ocupado por reserva online (`ANULADA`) y compatibilidad de prestación.
- **Dependencias**: `C-12, C-14`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 4
  - `knowledge-base/05_reglas_de_negocio.md` §RN-LE, §RN-TU-06
  - `knowledge-base/04_modelo_de_datos.md` §inscripcion_espera, §oferta_hueco, §oferta_envio
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-08, SU-04, SU-05
  - `knowledge-base/10_preguntas_abiertas.md` IN-01

### [C-18] `lista-espera-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m3
- **Resultado**: el paciente se inscribe, acepta una oferta o ve el aviso "ya fue tomado" o "expiró", y el consultorio ve inscriptos y ofertas. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/publico/lista-espera/`, `frontend/src/pages/panel/lista-espera/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - `/c/{slug}/lista-espera`: formulario con rango de fechas y prestación opcional.
  - `/o/{token}`: oferta, "Quiero este turno" y estados tomado o expirado.
  - Página de baja.
  - Panel: inscriptos activos y ofertas con su estado.
  - Horarios ofertados visibles en la agenda (`evento--oferta`).
- **Dependencias**: `C-13, C-16, C-17`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 6
  - `knowledge-base/07_flujos_principales.md` §Flujo 4
  - `design-system/components.html` §Oferta de lista de espera
  - `design-system/brand.md` §Tono de voz

---

## FASE 7 — Ficha clínica

> Puede avanzar en paralelo con las fases 4 a 6 desde C-10.

### [C-19] `ficha-clinica-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m4
- **Resultado**: el odontograma y las evoluciones se registran sin UPDATE ni DELETE posibles. El estado vigente se calcula por pieza y cara, y cada lectura de ficha queda auditada. Solo accede `ODONTOLOGO`.
- **Zona**: `backend/src/main/java/ar/turnos/clinica/`, `backend/src/main/resources/db/migration/`
- **Scope**:
  - Migración `V9__clinica.sql`: `odontograma_registro` y `evolucion_clinica` (append-only), con revocación de UPDATE y DELETE a nivel de base si el hosting lo permite.
  - `GET /api/pacientes/{id}/odontograma` (estado vigente e historial por pieza) y `POST .../odontograma/registros`.
  - `GET/POST /api/pacientes/{id}/evoluciones`, con rectificación por referencia.
  - FDI 11–48 y 51–85, con catálogo de estados provisorio (**PA-05**).
  - Auditoría `FICHA_LEIDA` (RN-CL-02).
  - Tests: inmutabilidad, permisos por rol y aislamiento.
- **Dependencias**: `C-10`
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §odontograma_registro, §evolucion_clinica
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CL, §RN-SG-03
  - `knowledge-base/09_decisiones_y_supuestos.md` DD-10, §Marco normativo
  - `knowledge-base/10_preguntas_abiertas.md` PA-05, PA-07

### [C-20] `ficha-clinica-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m3
- **Resultado**: el odontólogo marca estados por pieza y cara, ve el historial y escribe evoluciones desde la ficha del paciente. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/panel/pacientes/ficha/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - Odontograma interactivo (permanente, temporario o mixto), con selección por cara y leyenda.
  - Historial por pieza.
  - Evoluciones asociadas al turno.
  - Accesible por teclado.
- **Dependencias**: `C-13, C-19`
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 7
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CL
  - `design-system/components.html` §Odontograma
  - `knowledge-base/10_preguntas_abiertas.md` PA-05

---

## FASE 8 — Reportes, auditoría, exportación y administración

### [C-21] `reportes-auditoria-exportacion-api`
- **Estado**: `[ ]` pendiente
- **Responsable**: m5
- **Resultado**: el reporte de ausentismo coincide con RN-AS-02 en datos de prueba, la auditoría se consulta por rango, y la exportación descarga un ZIP completo del consultorio y queda auditada.
- **Zona**: `backend/src/main/java/ar/turnos/reportes/`, `backend/src/main/java/ar/turnos/auditoria/`
- **Scope**:
  - `GET /api/reportes/ausentismo?desde=&hasta=`: atendidos, ausentes, cancelaciones por actor, liberados y reasignados por lista de espera (US-070).
  - `GET /api/auditoria?desde=&hasta=` (US-071).
  - `GET /api/exportacion`: ZIP con un CSV o JSON por entidad (pacientes, turnos, ficha, auditoría), disponible aunque el consultorio esté suspendido (RN-SG-04, US-072).
  - Tests: cálculo de la tasa y contenido y aislamiento de la exportación.
- **Dependencias**: `C-12, C-17, C-19`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 8
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AS, §RN-SG-02, §RN-SG-04
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito
  - `knowledge-base/04_modelo_de_datos.md` §auditoria

### [C-22] `reportes-auditoria-ui`
- **Estado**: `[ ]` pendiente
- **Responsable**: m4
- **Resultado**: el odontólogo ve sus indicadores por período, consulta la auditoría y descarga su exportación. Coincide con el mockup aprobado.
- **Zona**: `frontend/src/pages/panel/reportes/`
- **Scope**:
  - **Requiere un mockup aprobado** antes de implementar.
  - Indicadores de ausentismo y recuperados (estilo `kpi` del design-system), con filtro de período.
  - Tabla de auditoría.
  - Botón de exportar.
- **Dependencias**: `C-13, C-21`
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 8
  - `design-system/components.html` §Panel del consultorio
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito

### [C-23] `admin-saas`
- **Estado**: `[ ]` pendiente
- **Responsable**: m1
- **Resultado**: el `ADMIN_SAAS` lista consultorios, cambia el plan, suspende y reactiva, y ve métricas de plataforma y consumo de la cuota de email, sin acceso a datos de pacientes.
- **Zona**: `backend/src/main/java/ar/turnos/admin/`, `frontend/src/pages/admin/`
- **Scope**:
  - `GET /api/admin/consultorios` y `PATCH /api/admin/consultorios/{id}` para plan y estado (RN-PL-02, RN-MT-03).
  - `GET /api/admin/metricas`: consultorios activos, turnos por mes, emails enviados y fallidos, cuota.
  - Página de administración simple, interna, que no requiere mockup público: confirmar con el equipo si aplica la política de mockups.
  - Tests: RN-SG-03 (sin datos de pacientes) y auditoría.
- **Dependencias**: `C-07, C-14`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 9
  - `knowledge-base/05_reglas_de_negocio.md` §RN-PL, §RN-MT-03, §RN-SG-03
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/11_notificaciones_email.md` §Estimación de capacidad

---

## FASE 9 — Lanzamiento

### [C-24] `lanzamiento-produccion`
- **Estado**: `[ ]` pendiente
- **Responsable**: m1
- **Resultado**: el MVP queda en producción con un consultorio piloto. Prueba de humo de punta a punta: reservar, recibir el email, cancelar, aceptar la oferta en la lista de espera y registrar asistencia. Copias de seguridad verificadas y checklist legal revisado.
- **Zona**: `docs/`, `backend/src/main/resources/`, `frontend/public/`
- **Scope**:
  - Prueba de humo de punta a punta en producción con datos ficticios.
  - Headers de seguridad: CSP, HSTS y `Referrer-Policy` en el hosting estático.
  - SPF, DKIM y DMARC si hay dominio propio (PA-08).
  - Copias de seguridad verificadas o `pg_dump` cifrado (PA-13).
  - Límites del plan gratuito definidos (PA-01).
  - Checklist legal (PA-07): términos, política de privacidad, contrato de encargado del tratamiento.
  - Alta del consultorio piloto.
  - Medición de la línea base de ausentismo.
- **Dependencias**: `C-03, C-16, C-18, C-20, C-23`
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/12_despliegue_y_entornos.md`
  - `knowledge-base/10_preguntas_abiertas.md` PA-01, PA-07, PA-08, PA-13
  - `knowledge-base/11_notificaciones_email.md` §Entregabilidad
  - `knowledge-base/01_vision_y_objetivos.md` §Métricas de éxito
