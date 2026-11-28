# Design

## Context

La motivación está en `proposal.md`. El estado real del backend (C-01, fusionado) condiciona el enfoque:

- Spring Boot 4.2.0, Java 17 y **Hibernate 7.4.15** (la versión que resuelve Gradle con el BOM de Boot 4.2.0, verificada con `dependencyInsight`).
- `ddl-auto: validate`: toda entidad mapeada tiene que coincidir con su migración. Las tablas sin entidad no se validan.
- `V1__extensiones.sql` ya aplicada, sin cambios.
- `SecurityConfig` cierra todo por defecto y no hay login hasta C-06.
- `AbstractIntegrationTest` + `TestcontainersConfiguration` (`postgres:15`, contexto compartido) están en `src/test/java/ar/turnos/shared/`.

Zona de C-04: `shared/`, `auditoria/` y `db/migration/`. Las entidades `Consultorio`, `Usuario` y `Sillon` corresponden a los paquetes `consultorio` y `auth`, que están en la zona de C-06.

## Goals / Non-Goals

**Goals:**
- Un mecanismo de tenancy *fail-closed*: sin consultorio en el contexto, una entidad de negocio no se puede leer ni escribir.
- Que las fichas siguientes solo tengan que extender una clase base y su suite de aislamiento.
- Un registro de auditoría que ni la aplicación ni el SQL directo puedan alterar.

**Non-Goals:**
- Autenticación, sesión, roles y `@PreAuthorize` (C-06).
- Resolver el consultorio desde el slug o el token de las rutas públicas (C-14 y C-15). Se usa la misma API de contexto.
- Endpoints, y el mapeo HTTP de las violaciones de tenancy (ver Open Questions).
- Entidades JPA de `consultorio`, `usuario` y `sillon` (C-06), y el alta transaccional del consultorio (C-06).
- Consultar y exportar la auditoría (C-21).

## Decisions

### D1 — Filtro de Hibernate con `@FilterDef` auto-habilitado
Siguiendo KB 08 §Multi-tenancy, `BaseTenantEntity` (`@MappedSuperclass`, en `shared.tenancy`) declara:

```java
@FilterDef(name = "tenant",
    parameters = {
        @ParamDef(name = "consultorioId", type = UUID.class, resolver = ConsultorioActualResolver.class),
        @ParamDef(name = "modoSistema", type = Boolean.class, resolver = ModoSistemaResolver.class) },
    defaultCondition = "(:modoSistema = true or consultorio_id = :consultorioId)",
    autoEnabled = true,
    applyToLoadByKey = true)
@Filter(name = "tenant")
```

- `autoEnabled` (Hibernate 7) habilita el filtro en **toda** sesión. Hibernate obtiene los parámetros de los `resolver` al ejecutar cada consulta (`FilterImpl.getParameterValue`, verificado en el código fuente de 7.4). `ConsultorioActualResolver` lee `TenantContext`:
  - Con un consultorio establecido, devuelve su id y `ModoSistemaResolver` devuelve `false`.
  - En modo sistema (D2), `ModoSistemaResolver` devuelve `true` y el id es el UUID nulo `00000000-…`. No es `null`, para no depender de cómo Hibernate enlaza un parámetro nulo.
  - Sin consultorio y fuera del modo sistema, lanza `TenantContextAusenteException`: es *fail-closed*.
- `applyToLoadByKey` hace que `find()` y las asociaciones a uno también respeten el filtro (requisito "Búsqueda por id ajeno").
- **Esto reemplaza al "filtro o interceptor que habilita el filtro por request" de la ficha.** Con `autoEnabled`, el filtro queda habilitado sin depender de la capa web, también en tareas `@Scheduled` y en tests. Un filtro web solo agregaría un lugar donde olvidarse de habilitarlo.
- **Alternativa descartada**: un `OncePerRequestFilter` que llame a `session.enableFilter`. No cubre el trabajo fuera de una request y deja una ventana sin filtro si alguien abre una sesión antes.
- **Alternativa descartada**: `@TenantId` (multi-tenancy por discriminador nativa de Hibernate). Es más automática, pero contradice KB 08 y DD-01, que eligen `@FilterDef`. Cambiarla es una decisión del equipo (ver Open Questions).

### D2 — `TenantContext` explícito y con alcance acotado
Va en `shared.tenancy`. Es un `ThreadLocal<UUID>` con esta API:

- `TenantContext.ejecutarCon(UUID consultorioId, Supplier<T>)` y su variante `Runnable`: establece el consultorio, ejecuta y **siempre** restaura el valor anterior en un `finally`. No hay `set` público suelto, para que el contexto no se filtre a la siguiente request del mismo hilo del pool.
- `TenantContext.actual()`: `Optional<UUID>`. `TenantContext.requerido()`: lanza `TenantContextAusenteException` si no hay consultorio.
- **Mientras no haya login**: los tests y los servicios internos llaman a `ejecutarCon` de forma explícita.
- **C-06**: un filtro web propio de `auth` envuelve cada request autenticada con `ejecutarCon(usuario.consultorioId, …)`. Las rutas públicas (C-14, C-15) hacen lo mismo con el consultorio resuelto por slug o token. Ningún controller recibe `consultorio_id` del cliente (RN-MT-02).
- El `ThreadLocal` no se propaga a otros hilos (`@Async`, ejecutores): quien cambie de hilo vuelve a llamar a `ejecutarCon`.

**Modo sistema explícito** (spec "Modo sistema explícito y acotado"):

- `TenantContext.comoSistema(Supplier<T>)` y su variante `Runnable` marcan el hilo en modo sistema solo durante el bloque y restauran el estado anterior en un `finally`, igual que `ejecutarCon`. No hay otra forma de activarlo: ni una propiedad de configuración, ni un `set`, ni un valor por defecto.
- Dentro del bloque, el filtro deja pasar filas de todos los consultorios. `requerido()` sigue lanzando, porque no hay un consultorio actual. `ejecutarCon` anidado vuelve a filtrar por ese consultorio hasta terminar.
- **Guardas en modo sistema**: `@PrePersist` exige que la entidad ya traiga su `consultorio_id`, asignado por el servidor (por ejemplo, el de una fila recién leída). `@PreUpdate` y `@PreRemove` lo permiten, porque el bloque es explícito.
- **Usos previstos**, que deciden y documentan sus fichas:
  - C-06: buscar el usuario por email al iniciar sesión y en la recuperación de contraseña, antes de conocer el consultorio, y después pasar a `ejecutarCon(usuario.consultorioId)`. También el login del `ADMIN_SAAS`, que no tiene consultorio.
  - C-15 y C-17: buscar el token de gestión o de oferta por su hash.
  - C-05 y C-23: procesos que recorren todos los consultorios, si no usan SQL nativo.
  - Buscar por slug no lo necesita: `consultorio` es el tenant y no tiene `consultorio_id`, así que no lleva el filtro.
- **Por qué un bloque y no un flag**: un bloque con alcance hace que el modo sistema sea visible en el código y se pueda encontrar con una búsqueda (`comoSistema(`). Además, no puede quedar activo por olvido en un hilo del pool.
- **Alternativa descartada**: deshabilitar el filtro en la `Session` actual (`session.disableFilter`). Ata el modo a una sesión concreta, que no siempre existe al entrar al bloque, y vuelve a habilitarse solo con la próxima sesión, así que es fácil equivocarse.

### D3 — Guardas de escritura con callbacks JPA
En `BaseTenantEntity`:

- `consultorio_id` se mapea como `UUID` (no como relación con `Consultorio`, que todavía no es entidad), con `updatable = false` y sin *setter* público.
- `@PrePersist`: si `consultorioId` es null, toma `TenantContext.requerido()`. Si viene con otro valor, lanza `TenantViolationException`.
- `@PreUpdate` y `@PreRemove`: si `consultorioId` no coincide con `TenantContext.requerido()`, lanzan `TenantViolationException`.
- **Por qué**: el filtro solo restringe lecturas. Las guardas cubren la entidad obtenida por otra vía (por ejemplo, con `merge` de un objeto armado a mano).

### D4 — Migraciones V2 y V3
- **Tipos enumerados**: `varchar` + `CHECK (col IN (…))` en lugar de `CREATE TYPE … AS ENUM`. Se mapean con `@Enumerated(STRING)` y pasan `validate` sin tipos JDBC especiales, y agregar un valor más adelante es un `ALTER … CHECK` simple.
- **`consultorio`**: `id`, `nombre`, `slug` UNIQUE, `email_contacto`, `telefono`, `direccion`, `zona_horaria` (default de KB 04), `plan` (default `GRATUITO`), `estado`, `created_at` y `updated_at`. Las tres columnas de reglas de reserva **no** van acá: las agrega `V6` (scope de C-08).
- **`usuario`**:
  - Columnas de KB 04.
  - `CHECK ((rol = 'ADMIN_SAAS') = (consultorio_id IS NULL))`.
  - Índice por `consultorio_id`.
  - `email` UNIQUE. La normalización a minúsculas la hace C-06 al registrar.
- **`sillon`**:
  - `id`, `consultorio_id` NOT NULL, `nombre` con default `'Sillón 1'`, `activo` (NOT NULL DEFAULT true), `created_at` y `updated_at`.
  - `UNIQUE (consultorio_id)`, que hace cumplir el "no se pueden agregar más" de DD-05. Admitir varios sillones en el futuro es un `DROP CONSTRAINT`.
- **`auditoria`**:
  - Columnas e índice (`consultorio_id`, `created_at DESC`) de KB 04, `actor_tipo` NOT NULL y `CHECK ((actor_tipo = 'USUARIO') = (usuario_id IS NOT NULL))`.
  - Un trigger `BEFORE UPDATE OR DELETE … FOR EACH ROW` que lanza una excepción, para hacer cumplir el append-only también ante SQL directo.
- Claves UUID generadas por la aplicación (`@UuidGenerator`), sin default en la base. `created_at` y `updated_at` son NOT NULL DEFAULT `now()`.

### D5 — `AuditService` y actor extensible
En `ar.turnos.auditoria`:

- `Auditoria` es una entidad `@Immutable` que **no** extiende `BaseTenantEntity`: su `consultorio_id` es nullable (acciones de sistema o de `ADMIN_SAAS`), y su lectura queda para C-21.
- `datos` es un `String` con `@JdbcTypeCode(SqlTypes.JSON)`. `AuditService` lo serializa con el `JsonMapper` de Spring (Jackson 3). Así se evita depender del *format mapper* JSON de Hibernate.
- `ip` usa `@JdbcTypeCode(SqlTypes.INET)` sobre `InetAddress` y se toma de `RequestContextHolder` si hay request (`getRemoteAddr()`); si no, null.
- `registrar(accion, entidad, entidadId, datos)` es `@Transactional(propagation = REQUIRED)`, así que se une a la transacción de quien llama (requisito de atomicidad).
- El actor viene de una interfaz `ActorAuditoriaProvider`. En C-04, el bean por defecto (`@ConditionalOnMissingBean`) devuelve `SISTEMA` sin usuario. C-06 aporta su implementación desde `auth` sin editar `auditoria`.
- El servicio no escribe en el log el `datos` de la acción: como máximo, la acción y el id.

### D6 — Suite de aislamiento reutilizable
- `AbstractTenantIsolationTest extends AbstractIntegrationTest`, en `src/test/java/ar/turnos/shared/tenancy/`:
  - En `@BeforeEach` crea dos consultorios ficticios por JDBC (`consultorio-prueba-a-<sufijo>` y `…-b-<sufijo>`, con un sufijo aleatorio para respetar el UNIQUE con el contexto compartido).
  - Expone `consultorioA()`, `consultorioB()` y `comoConsultorio(UUID, Supplier<T>)`, que corre dentro de una transacción.
- Para probar el mecanismo sin entidades de dominio, se usa una entidad **solo de test**, `EntidadPruebaTenant extends BaseTenantEntity`, sobre la tabla `prueba_tenant` (`id`, `consultorio_id` y un `valor` varchar único para el escenario de modo sistema). La crea la migración de test `src/test/resources/db/testmigration/V1000__prueba_tenant.sql`.
- `AbstractIntegrationTest` agrega `properties = "spring.flyway.locations=classpath:db/migration,classpath:db/testmigration"`, para que todos los tests de integración compartan **un solo** contexto y un solo contenedor.

**Cómo se aíslan las migraciones de test** (revisado contra los tests de C-01):

| Test de C-01 | Qué verifica | Por qué no se rompe |
|---|---|---|
| `SchemaMigrationsIntegrationTest` (extiende `AbstractIntegrationTest`) | Que `flyway_schema_history` tenga la versión `'1'` aplicada con éxito, que `btree_gist` esté instalada y que `ddl-auto` sea `validate` | Cuenta solo `version = '1'`, así que sumar V2, V3 y V1000 no cambia el resultado. `EntidadPruebaTenant` coincide con `prueba_tenant`, así que la validación del esquema pasa |
| `FlywayChecksumIntegrationTest` (no usa Spring) | Que una migración modificada después de aplicarse haga fallar `validate()` | Crea su propio contenedor y configura Flyway a mano con `filesystem:` sobre un `@TempDir`. No lee `spring.flyway.locations` ni ve `db/migration` ni `db/testmigration` |

- `db/testmigration` vive en `src/test/resources`: Gradle no lo empaqueta en `build/resources/main` ni en el jar, así que en `dev` y `prod` Flyway nunca la ve.
- Versión `V1000`: es mayor que cualquier número reservado en `CHANGES.md` y no se mezcla con ellos. En la base efímera de Testcontainers se aplica siempre al final, después de V2, que crea la FK hacia `consultorio`. Si en algún momento se acercan las migraciones reales a V1000, se renumera la de test, que nunca llega a una base persistente.
- La migración de test no se puede editar como si estuviera aplicada en algún lado: cada corrida de tests arranca con un contenedor nuevo.

### D7 — Qué change crea cada entidad
Según las zonas reales de `CHANGES.md`, revisadas para este ajuste:

| Entidad / tabla | Tabla (migración) | Entidad JPA | Motivo |
|---|---|---|---|
| `consultorio` | C-04 (`V2`) | C-06, en `consultorio/` | C-06 tiene `consultorio/` en su zona y su alta transaccional crea el consultorio |
| `usuario` | C-04 (`V2`) | C-06, en `auth/` | Login, registro y recuperación son de C-06 |
| `sillon` | C-04 (`V2`) | C-06, en `consultorio/` | El alta transaccional de C-06 crea `Sillón 1` |
| `auditoria` | C-04 (`V3`) | C-04, en `auditoria/` | Zona de C-04 |
| `notificacion` | C-05 (`V4`) | C-05, en `notificaciones/` | Zona de C-05 |

**C-05 no necesita esas entidades**, y la conclusión no cambia:

- Su zona es `notificaciones/`, `templates/email/` y `db/migration/`. No incluye `consultorio/` ni `auth/`.
- `notificacion` solo guarda `consultorio_id` (FK), `destinatario` (un email en texto) y `payload`. No referencia a `usuario` ni a `sillon`.
- Del consultorio solo necesita leer el `nombre` (remitente "Consultorio X vía <producto>"), el `email_contacto` (`Reply-To`, KB 11) y el `estado` (RN-MT-03: un consultorio suspendido no envía emails). El procesador ya usa SQL nativo (`FOR UPDATE SKIP LOCKED`), así que esos campos se leen con un JOIN en esa misma consulta, desde `notificaciones/`, sin entidad JPA.
- Sus tests necesitan una fila de `consultorio`, y `AbstractTenantIsolationTest` la crea por JDBC.
- **Restricción para C-05**: no crea una entidad `Consultorio` propia (quedaría fuera de su zona y duplicaría la de C-06). Si en algún momento necesita más que esos campos, se coordina con C-06.

**Nota para C-06 sobre `usuario`**: su `consultorio_id` es nullable (`ADMIN_SAAS`), así que no puede extender `BaseTenantEntity`, que lo exige no nulo. Para que el listado de usuarios quede filtrado por la capa de persistencia (RN-MT-01), la entidad `Usuario` declara `@Filter(name = "tenant")` directamente: el `@FilterDef` es global por nombre. Con eso, una fila `ADMIN_SAAS` nunca aparece bajo un consultorio. La búsqueda por email del login usa el modo sistema (D2).
- Cada ficha posterior extiende `AbstractTenantIsolationTest` y repite los casos de listado, búsqueda por id, alta y modificación con su propia entidad.
- **Alternativa descartada**: mapear una entidad de test sobre `sillon`. Acopla los tests a una tabla que C-06 va a mapear y C-08 puede modificar.

## Risks / Trade-offs

- [Las consultas SQL nativas no pasan por el filtro de Hibernate] → regla documentada en el `package-info` de `shared.tenancy` y en el handoff: toda consulta nativa sobre tablas de negocio incluye `consultorio_id = :consultorioId` con `TenantContext.requerido()`. C-05 (`FOR UPDATE SKIP LOCKED`) es el primer caso.
- [`applyToLoadByKey` es `@Incubating` en Hibernate 7.4] → un test específico cubre `find()` de un id ajeno. Si una versión futura lo cambia, el respaldo es un `@PostLoad` que compara el consultorio.
- [Filtro y UPDATE/DELETE masivos en JPQL] → **verificado en la implementación (Hibernate 7.4.15)**: el filtro SÍ se aplica a un UPDATE masivo en JPQL (`UpdateMasivoJpqlIntegrationTest`: en el contexto de A afecta 1 fila y la de B queda intacta). No hace falta una regla extra; las consultas nativas siguen sin filtro.
- [El modo sistema abre la lectura de todos los consultorios] → solo existe como bloque explícito (`comoSistema(`), que se puede encontrar con una búsqueda. Los revisores de cada PR con zona de riesgo comprueban cada uso nuevo, y un test confirma que fuera del bloque se vuelve a fallar.
- [La condición `(:modoSistema = true or consultorio_id = :consultorioId)` puede impedir el uso del índice con planes genéricos de PostgreSQL] → con el volumen de v1 (consultorios de un sillón) no es relevante. Si aparece en un `EXPLAIN`, se pasa a dos `@FilterDef`, uno por modo.
- [El trigger append-only impide borrar un consultorio con auditoría] → es lo buscado. Hoy no hay ninguna regla de baja de consultorios.
- [Docker no está instalado en la máquina de la responsable] → igual que en C-01, los tests de integración se validan en CI.

## Migration Plan

- V2 y V3 son aditivas sobre V1 y no hay datos en producción todavía.
- Para revertir antes de fusionar, se descarta la rama. Después de fusionar, nunca se edita una migración aplicada: se crea una nueva.

## Open Questions

Ninguna bloquea C-04. Cada una tiene un valor por defecto aplicado en este diseño, y la decide el responsable indicado.

1. **Estado previo con datos personales** (RN-SG-02 vs. RN-SG-06): ¿`auditoria.datos` puede contener DNI o datos clínicos? RN-SG-06 prohíbe esos datos en logs, emails y URLs, pero no en la tabla de auditoría. *Por defecto*: se guardan en la tabla y nunca se escriben en el log. Decide el equipo antes de C-12 y C-19.
2. **IP real detrás del reenvío `/api/*`**: sin `server.forward-headers-strategy`, `getRemoteAddr()` devuelve la IP del proxy. *Por defecto*: se usa `getRemoteAddr()` y la configuración de cabeceras reenviadas queda para C-03.
3. **Respuesta HTTP ante una violación de tenancy**: C-04 no expone endpoints, así que hoy esas excepciones caen en el 500 genérico. *Propuesta para C-06/C-08*: que un recurso ajeno responda 404, para no revelar que existe. No está definido en la KB.
4. **`@TenantId` en lugar de `@FilterDef`**: Hibernate 7 ofrece multi-tenancy por discriminador, que también filtra y asigna en los inserts. *Por defecto*: `@FilterDef`, como manda KB 08. Cambiarlo requiere una nueva DD.
5. **Métricas agregadas de `ADMIN_SAAS`** (RN-SG-03): C-23 puede usar el modo sistema o SQL nativo. Tiene que asegurar que no lee datos de pacientes ni fichas, algo que el modo sistema no impide por sí solo.
