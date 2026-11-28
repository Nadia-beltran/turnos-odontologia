# Tasks

> Governance ALTO: antes de empezar, una persona confirma las Open Questions (H) de `design.md` (intentos, tipos y tick). Strict TDD: cada tarea arranca con el test en rojo (RED), después el código mínimo (GREEN) y al final la limpieza (REFACTOR), corriendo los tests en cada paso. Los tests de integración necesitan Docker; sin Docker quedan omitidos (`disabledWithoutDocker`) y se validan en CI, igual que en C-01 y C-04.

## 1. Infraestructura de tests y dependencias (design D6 y D8)

- [x] 1.1 Safety net: correr `./gradlew test` en la rama y anotar el baseline (cantidad de tests en verde). Si algo falla, frenar y reportarlo como falla previa
- [x] 1.2 Agregar `org.thymeleaf:thymeleaf` (sin versión, la gestiona el BOM) a `build.gradle` y verificar con `./gradlew dependencyInsight --dependency thymeleaf` que la resuelve el BOM de Boot 4.2.0 y que no entra `thymeleaf-spring*`
- [x] 1.3 Sumar a `TestcontainersConfiguration` un `GenericContainer` de Mailpit con versión fija (puertos 1025 y 8025) y un `DynamicPropertyRegistrar` para `spring.mail.host`/`port`; sumar `app.tick.programado=false` a las propiedades de `AbstractIntegrationTest`; crear en tests un cliente mínimo de la API de Mailpit (listar, leer y vaciar mensajes). Verificar con un test de humo que envía un email con `JavaMailSender` y lo encuentra en Mailpit, y que los tests de C-01 y C-04 siguen en verde (mismo baseline de 1.1)

## 2. Esquema y encolado (spec `email-notifications`: encolado, validación y aislamiento; design D1 y D4)

- [x] 2.1 RED: escribir `NotificacionSchemaIntegrationTest extends AbstractIntegrationTest` (JDBC, datos ficticios) que pruebe que V4 está aplicada y que la base rechaza: `tipo` desconocido, `prioridad` 0 o 5, `estado` desconocido, `intentos` negativo y `ENVIADA` sin `enviada_at` (y `enviada_at` con otro estado). GREEN: crear `V4__notificacion.sql` según design D1. Verificar que el test pasa y que `git diff --stat main -- backend/src/main/resources/db/migration/V1__extensiones.sql V2__tenancy.sql V3__auditoria.sql` está vacío
- [x] 2.2 RED: escribir `TipoNotificacionTest` (unitario) que fije la prioridad de cada tipo según design D4 (al menos uno por prioridad y `OFERTA_TOMADA` = 4). GREEN: crear el enum `TipoNotificacion` (prioridad y asunto) y `EstadoNotificacion`. Verificar con `./gradlew test --tests '*TipoNotificacionTest'`
- [x] 2.3 RED: escribir `NotificacionServiceIntegrationTest extends AbstractTenantIsolationTest` con los escenarios "Evento confirmado", "Evento revertido", "Sin fecha programada", "Destinatario inválido", "Tipo sin plantillas" (requisitos "Encolado transaccional" y "Datos de encolado válidos"), usando plantillas solo de test en `src/test/resources/templates/email/` para un tipo de cada prioridad. GREEN: crear la entidad `Notificacion` (con `@Filter(name = "tenant")`, guardas propias y `payload` JSON, design D1), su repositorio y `NotificacionService.encolar(tipo, destinatario, payload, programadaPara)` con validación y chequeo de plantillas. Verificar que el test pasa en CI
- [x] 2.4 RED→GREEN: extender `NotificacionServiceIntegrationTest` (o una clase de aislamiento propia) con los escenarios del requisito "Notificación aislada por consultorio": listado propio, `find()` de una notificación ajena devuelve vacío, encolado sin contexto rechazado, encolado en modo sistema sin consultorio. Verificar que pasa en CI
- [x] 2.5 REFACTOR: documentar en el `package-info` de `notificaciones` cómo encolar (siempre dentro de la transacción del evento, nunca DNI ni datos clínicos en el `payload`, plantillas por tipo). Verificar que los tests del grupo siguen en verde

## 3. Plantillas y envío SMTP (spec `email-notifications`: contenido y remitente; design D6 y D7)

- [x] 3.1 RED: escribir `PlantillasEmailTest` (unitario, plantillas de test) que verifique que el HTML escapa `<b>Ana</b>`, que el texto plano lo contiene sin escapar, que se inyectan `consultorio`, `marca` y `baseUrl`, y que el layout no tiene `<img src="http`. GREEN: crear `PlantillasEmail` (motor Thymeleaf con resolvers HTML y TEXT, design D6) y `templates/email/_layout.html`. Verificar con `./gradlew test --tests '*PlantillasEmailTest'`
- [x] 3.2 RED: escribir `EnviadorSmtpIntegrationTest extends AbstractIntegrationTest` que envíe un `EmailSaliente` y verifique en Mailpit las escenas "Email multiparte", "Email de un consultorio" (remitente "Consultorio Prueba vía Estudio Sonrisa" y `Reply-To`) y "Email del sistema" (solo la marca, sin `Reply-To`). GREEN: crear el puerto `EnviadorEmail`, `EmailSaliente` y `EnviadorSmtp`; sumar `app.brand-name` y los timeouts SMTP de 10 s a `application.yml`. Verificar que pasa en CI

## 4. Procesador del outbox (spec `email-notifications`: envío, prioridad, cuota, reintentos, suspendidos, concurrencia y logs; design D3, D4 y D5)

- [x] 4.1 RED: crear `ProcesadorNotificacionesIntegrationTest extends AbstractTenantIsolationTest` con `app.mail.daily-quota=10` y `@MockitoSpyBean EnviadorEmail`, con los escenarios "Envío exitoso" y "Notificación futura". GREEN: crear `ProcesadorNotificaciones` (modo sistema, una transacción, selección nativa con `FOR UPDATE OF n SKIP LOCKED` y JOIN a `consultorio`, design D3), el bean `Clock` y `app.mail.lote`. Verificar que pasa en CI
- [x] 4.2 RED→GREEN: sumar "Prioridad antes que antigüedad", "Cuota agotada", "Umbral del 80 %", "Cuota que se agota en el lote" y "Día nuevo" (enviados insertados por JDBC con `enviada_at` de hoy o de ayer en UTC), e implementar el cálculo de cuota de design D4. Triangular con al menos un caso de prioridad 1 en el umbral del 80 %. Verificar que pasa en CI
- [x] 4.3 RED→GREEN: sumar "Primer fallo" (reprogramada a +5 min, con tolerancia), "Reintento exitoso", "Fallo definitivo" (4 intentos, `FALLIDA`, no se reenvía) y "Proveedor caído", con `doThrow` en el spy; triangular las esperas de 15 y 60 min. Implementar el modelo de reintentos de design D5. Verificar que pasa en CI
- [x] 4.4 RED→GREEN: sumar "Consultorio suspendido" y "Consultorio pendiente de verificación" (estados cambiados por JDBC) y una notificación sin consultorio que sí se envía. Verificar que pasa en CI
- [x] 4.5 RED→GREEN: sumar "Procesamientos simultáneos" (dos hilos llaman al procesador a la vez; Mailpit recibe un solo email) e implementar el `pg_try_advisory_xact_lock` de design D3. Verificar que pasa en CI de forma estable (correrlo 3 veces seguidas)
- [x] 4.6 RED→GREEN: sumar "Envío fallido registrado" con `OutputCaptureExtension` (el log tiene el id y no tiene el destinatario ni el contenido ni el mensaje del error) y la advertencia diaria de cuota agotada. Verificar que pasa en CI
- [x] 4.7 REFACTOR: Javadoc del procesador con la excepción explícita a la regla de consultas nativas (design D3) y limpieza de constantes (esperas, umbral). Verificar que todo el grupo sigue en verde

## 5. Tareas periódicas y tick (spec `periodic-tasks`; design D2)

- [x] 5.1 RED: escribir `TickRunnerTest` (unitario) con el escenario "Una tarea falla" (la segunda tarea corre igual y el log tiene el nombre de la tarea, no el mensaje). GREEN: crear `ar.turnos.shared.tick.TareaPeriodica` y `TickRunner`. Verificar con `./gradlew test --tests '*TickRunnerTest'`
- [x] 5.2 RED: escribir `TickIntegrationTest extends AbstractTenantIsolationTest` con "Tick procesa el outbox" y "Tick repetido" (llamando a `TickRunner.ejecutar()`). GREEN: hacer que `ProcesadorNotificaciones` implemente `TareaPeriodica`. Verificar que pasa en CI
- [x] 5.3 RED: escribir un test de contexto que verifique que con `app.tick.programado=true` existe el bean `TickProgramado` con `@Scheduled` de 5 minutos y que con `false` no existe. GREEN: crear `TickProgramado` (`@EnableScheduling`, `fixedDelayString = "${app.tick.intervalo:PT5M}"`) y sumar `app.tick.programado`/`intervalo` a `application.yml`. Verificar que pasa en CI

## 6. Verificación integral y entrega

- [x] 6.1 Correr `./gradlew build` en CI y verificar todo en verde, con el baseline de 1.1 intacto más los tests nuevos. Probar a mano en `dev` (`docker compose up -d`, `./gradlew bootRun`): encolar desde un test o un `CommandLineRunner` temporal no commiteado y ver el email en http://localhost:8025 (nota: la prueba manual en dev con bootRun no se hizo; la cubren los tests de integración contra Mailpit real. Build: 108 tests, 0 fallos)
- [ ] 6.2 Cableado del tick (design D2): si C-03 ya está en `main`, agregar `tickRunner.ejecutar()` a su controller de `POST /api/internal/tick` y un test MockMvc con `X-Tick-Secret` válido que deja una notificación `ENVIADA`. Si no está, dejar el paso escrito en el handoff para C-03. Verificar que C-05 no crea el endpoint ni toca `SecurityConfig` (`git diff --stat main -- backend/src/main/java/ar/turnos/shared/security/` vacío) (DIFERIDA: C-03 no está en main; el cableado queda escrito en knowledge-base/entrega/C-05.md; verificado que git diff de shared/security está vacío)
- [x] 6.3 Escribir el handoff `knowledge-base/entrega/C-05.md`: cómo encolar un tipo nuevo (enum, plantillas, CHECK de V4 en una migración nueva), cómo sumar una `TareaPeriodica`, el cableado pendiente del tick, las correcciones de KB 04 (`prioridad`, `consultorio_id` nullable, tipos) y las Open Questions del diseño con su decisión. Verificar que nombra todas las clases públicas de `notificaciones` y `shared.tick`
- [x] 6.4 Correr `openspec validate c-05-notificaciones-outbox --strict` y verificar que da válido

## Workflow follow-up

- Abrir el PR desde `C-05-notificaciones-outbox`, con revisión de m2 (C-06, siguiente ficha) y de m1 por el contrato del tick (governance ALTO). Commit y push solo con autorización de la persona.
- Al fusionar, marcar `[x]` en la ficha C-05 de `CHANGES.md` y archivar el change.
