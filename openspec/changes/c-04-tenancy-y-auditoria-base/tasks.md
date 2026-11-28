# Tasks

> Los tests de integración necesitan Docker. Sin Docker en local quedan omitidos (`disabledWithoutDocker`) y se validan en CI, igual que en C-01. En local se verifica al menos `./gradlew compileJava compileTestJava` y los tests unitarios.

## 1. Esquema de tenancy (spec `tenant-isolation`, design D4)

- [ ] 1.1 Crear `backend/src/main/resources/db/migration/V2__tenancy.sql` con `consultorio`, `usuario` y `sillon` según design D4: CHECK de enumerados, CHECK rol/consultorio, `UNIQUE (consultorio_id)` en `sillon` e índices por `consultorio_id`, sin las columnas de reglas de C-08. Verificar que `V1__extensiones.sql` no cambió (`git diff --stat main -- backend/src/main/resources/db/migration/V1__extensiones.sql` vacío)
- [ ] 1.2 Escribir `TenancySchemaIntegrationTest extends AbstractIntegrationTest` (JDBC, datos ficticios), que cubra los escenarios: migraciones sobre base vacía (V2 registrada en `flyway_schema_history`), slug repetido, odontólogo con consultorio, administrador con consultorio, recepción sin consultorio y segundo sillón. Verificar que pasa en CI

## 2. Contexto de consultorio (design D2)

- [ ] 2.1 Crear `shared/tenancy/TenantContext` (`ejecutarCon` y `comoSistema`, cada uno con `Supplier` y `Runnable`, `actual()`, `requerido()` y `esModoSistema()`) y `TenantContextAusenteException` (design D2). Verificar con `TenantContextTest` (unitario, sin Spring) que:
  - Restaura el estado anterior tras una ejecución normal, con excepción y anidada (`ejecutarCon` dentro de `comoSistema` y al revés).
  - `requerido()` lanza sin contexto y también en modo sistema.
  - El modo sistema no queda activo después del bloque.
  - El estado no se ve desde otro hilo.

  Correr `./gradlew test --tests '*TenantContextTest'` en local

## 3. Filtro y guardas de escritura (spec `tenant-isolation`, design D1, D3 y D6)

- [ ] 3.1 Crear `shared/tenancy/BaseTenantEntity` con el `@FilterDef`/`@Filter` de design D1, los resolvers `ConsultorioActualResolver` y `ModoSistemaResolver`, las guardas `@PrePersist`/`@PreUpdate`/`@PreRemove` (incluidas sus reglas en modo sistema) y `TenantViolationException`. Documentar en `shared/tenancy/package-info.java` cómo extenderla, cuándo usar `comoSistema` y la regla de las consultas nativas. Verificar con `./gradlew compileJava`
- [ ] 3.2 Agregar la migración de test `src/test/resources/db/testmigration/V1000__prueba_tenant.sql` y la entidad de test `EntidadPruebaTenant extends BaseTenantEntity`, y sumar la ubicación `classpath:db/testmigration` en `AbstractIntegrationTest` (design D6). Verificar que el contexto arranca con `ddl-auto: validate`
- [ ] 3.3 Verificar que los tests de C-01 siguen pasando sin cambios en su código, con `./gradlew test --tests '*SchemaMigrationsIntegrationTest' --tests '*FlywayChecksumIntegrationTest' --tests '*ApiProblemDetailsTest' --tests '*RequiredEnvironmentTest'` en CI. Verificar también que la migración de test no se empaqueta: `./gradlew processResources` y que `build/resources/main/db/` no contiene `testmigration` ni `V1000`
- [ ] 3.4 Crear `AbstractTenantIsolationTest extends AbstractIntegrationTest` (dos consultorios ficticios con sufijo aleatorio, `consultorioA()`, `consultorioB()`, `comoConsultorio`) con un Javadoc que explique cómo extenderla en cada ficha. Verificar que compila (`./gradlew compileTestJava`)
- [ ] 3.5 Escribir `TenantIsolationIntegrationTest extends AbstractTenantIsolationTest` con `EntidadPruebaTenant`, que cubra los escenarios: listado propio, búsqueda por id ajeno (`find()` devuelve null), consulta sin contexto, alta en el contexto de A, alta con consultorio ajeno, modificación ajena (UPDATE y DELETE; la fila queda igual) y contexto restaurado. Verificar que pasa en CI
- [ ] 3.6 Escribir `ModoSistemaIntegrationTest extends AbstractTenantIsolationTest`, que cubra los escenarios de "Modo sistema explícito y acotado": búsqueda en modo sistema por `valor-sistema-1`; la misma búsqueda sin declarar el modo, que falla; el modo que no sobrevive al bloque, con salida normal y con excepción; y el consultorio dentro del modo sistema. Verificar que pasa en CI
- [ ] 3.7 Comprobar con un test si el filtro se aplica a un UPDATE masivo en JPQL sobre `EntidadPruebaTenant`. Registrar el resultado en `design.md` (riesgo correspondiente) y, si no se aplica, sumar la regla al `package-info` de `shared.tenancy`. Verificar que el test pasa en CI con el comportamiento que se documentó

## 4. Auditoría (spec `audit-log`, design D4 y D5)

- [ ] 4.1 Crear `backend/src/main/resources/db/migration/V3__auditoria.sql` con la tabla, el índice, el CHECK actor/usuario y el trigger append-only de design D4. Verificar que el contexto arranca con `ddl-auto: validate` una vez creada la entidad de 4.2
- [ ] 4.2 Crear en `ar.turnos.auditoria` la entidad `@Immutable` `Auditoria`, el enum `ActorTipo`, `ActorAuditoriaProvider` con su bean por defecto `SISTEMA` y `AuditService.registrar(accion, entidad, entidadId, datos)` (design D5), con validación del nombre de la acción (no vacío y de 60 caracteres como máximo). Verificar con `./gradlew compileJava`
- [ ] 4.3 Escribir `AuditServiceIntegrationTest extends AbstractTenantIsolationTest`, que cubra los escenarios: acción en el contexto de un consultorio (con `MockHttpServletRequest` y la IP `192.0.2.10` en `RequestContextHolder`), acción sin request ni consultorio, acción sin nombre, operación revertida, usuario faltante, intento de modificación, intento de borrado y registro con detalle (con `OutputCaptureExtension`, el log no contiene el detalle). Verificar que pasa en CI

## 5. Verificación integral y entrega

- [ ] 5.1 Correr `./gradlew build` en CI y verificar que todo está en verde, incluidos los tests de integración de los grupos 1, 3 y 4
- [ ] 5.2 Escribir el handoff `knowledge-base/entrega/C-04.md` (qué se hizo, cómo se verificó, cómo extender la suite de aislamiento, cuándo usar `comoSistema`, la regla de las consultas nativas, la tabla de qué change crea cada entidad de design D7 y las preguntas abiertas del diseño) y verificar que nombra todas las clases públicas de `shared.tenancy` y `auditoria`
- [ ] 5.3 Correr `openspec validate c-04-tenancy-y-auditoria-base --strict` y verificar que da válido

## Workflow follow-up

- Abrir el PR desde `C-04-tenancy-y-auditoria`, con revisión del responsable de la siguiente ficha y CODEOWNERS (governance CRÍTICO).
- Al fusionar, marcar `[x]` en la ficha C-04 de `CHANGES.md` y archivar el change.
