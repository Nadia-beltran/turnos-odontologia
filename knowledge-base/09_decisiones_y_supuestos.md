# Decisiones y Supuestos

## Decisiones documentadas

### DD-01 — Multi-tenant con esquema compartido
**Decisión**: una sola base y un solo esquema, con `consultorio_id` en cada tabla de negocio y un filtro automático de Hibernate.
**Contexto**: SaaS para muchos consultorios chicos, sin presupuesto.
**Alternativas consideradas**: un esquema por consultorio; una base por consultorio.
**Justificación**: es lo único viable en un plan gratuito de PostgreSQL (una base, conexiones limitadas). Además, las migraciones se aplican una sola vez.
**Trade-offs aceptados**: un error de filtrado expone datos de otro consultorio. Se mitiga con el filtro en la capa de persistencia, la resolución del consultorio solo en el servidor (RN-MT-02) y una suite de tests de aislamiento obligatoria.
**Decidido por**: la usuaria, el 2026-11-28.

### DD-02 — API y frontend desplegados por separado
**Decisión**: el frontend TS compilado se publica en un hosting estático y la API Spring Boot en otro servicio.
**Alternativas consideradas**: monolito que sirve también el frontend.
**Justificación**: despliegues independientes y frontend servido desde CDN.
**Trade-offs aceptados**: dos despliegues, y la necesidad de resolver la sesión entre dominios (DD-03).
**Decidido por**: la usuaria, el 2026-11-28.

### DD-03 — Mismo origen mediante reenvío de `/api`
**Decisión**: el hosting estático reenvía `/api/*` hacia el backend. El navegador ve un único origen.
**Contexto**: DD-02 con sesión por cookie (DD-11). Con dominios distintos, la cookie sería de terceros: Safari la bloquea y Chrome la restringe.
**Alternativas consideradas**: JWT en el header (obliga a guardar el token en JavaScript, expuesto ante XSS); dominio propio con subdominios (cuesta dinero).
**Justificación**: no tiene costo, no necesita CORS y mantiene la cookie `SameSite=Lax` de primera parte.
**Trade-offs aceptados**: dependemos de que el hosting estático soporte el reenvío a un dominio externo, y se suma un salto de red.
**Decidido por**: la usuaria, el 2026-11-28.

### DD-04 — Prevención de solapamientos en la base de datos
**Decisión**: restricción `EXCLUDE USING gist` sobre (`sillon_id`, rango de tiempo) para los turnos activos que no son sobreturno.
**Alternativas consideradas**: validar solo en el servicio; usar `SELECT … FOR UPDATE`.
**Justificación**: es la única garantía que resiste las carreras entre reservas online simultáneas y aceptaciones de lista de espera, aun ante errores de código.
**Trade-offs aceptados**: los tests de integración necesitan PostgreSQL real (Testcontainers), y la base debe soportar la extensión `btree_gist`.

### DD-05 — Entidad `sillon` con un único sillón por consultorio
**Decisión**: el sillón se modela como entidad, pero en v1 cada consultorio tiene exactamente uno.
**Justificación**: el producto apunta a odontólogos con un sillón. Modelarlo ahora evita migrar la restricción de solapamientos y la disponibilidad si en el futuro se admite más de uno.
**Trade-offs aceptados**: hay una tabla que hoy parece innecesaria.

### DD-06 — El email es el único canal con el paciente en v1
**Decisión**: confirmación, recordatorio, gestión y ofertas de la lista de espera se hacen por email.
**Contexto**: no hay presupuesto, y WhatsApp tiene costo por mensaje.
**Trade-offs aceptados**: el email llega a menos pacientes que WhatsApp (SU-03). WhatsApp queda como evolución posterior.
**Decidido por**: la usuaria, en el discovery.

### DD-07 — Prioridad en velocidad de entrega, con tests mínimos innegociables
**Decisión**: se prioriza la velocidad, pero se exigen tests sobre solapamiento, la regla de 24 h, la concurrencia de ofertas, el aislamiento entre consultorios y el cálculo de disponibilidad (ver 08).
**Justificación**: con 5 desarrolladores sobre el mismo código, las reglas críticas no pueden romperse en silencio.
**Decidido por**: la usuaria (velocidad), el 2026-11-28. El mínimo de tests es una propuesta aceptada en el mismo intercambio.

### DD-08 — Lista de espera: aviso a todos y el primero que acepta se queda con el turno
**Decisión**: cada turno liberado genera una oferta que se envía a todas las inscripciones compatibles, con un token por destinatario. La primera aceptación confirmada gana.
**Alternativas consideradas**: oferta en orden con plazo; decisión manual del consultorio.
**Justificación**: es lo más simple, no necesita estados intermedios temporizados y maximiza la probabilidad de reocupar el horario.
**Trade-offs aceptados**: los demás reciben un aviso que puede llegar tarde ("ya fue tomado"), y se consumen emails de la cuota.
**Decidido por**: la usuaria, el 2026-11-28.

### DD-09 — Emails mediante outbox transaccional
**Decisión**: los emails se encolan en `notificacion` dentro de la transacción del evento y los envía una tarea programada.
**Justificación**: no se pierden emails si el proveedor falla. Además permite respetar la cuota diaria del plan gratuito y priorizar los envíos.

### DD-10 — Ficha clínica append-only
**Decisión**: el odontograma y las evoluciones no se editan ni se borran: las correcciones son registros nuevos.
**Justificación**: la Ley 26.529 exige inalterabilidad de la historia clínica, y además da trazabilidad.

### DD-11 — Sesión con cookie de Spring Security
**Decisión**: sesión en el servidor con cookie `HttpOnly`, `Secure` y `SameSite=Lax`, más un token CSRF.
**Alternativas consideradas**: JWT sin estado.
**Decidido por**: la usuaria, el 2026-11-28.

### DD-12 — Recordatorio 48 h antes, configurable, con un mínimo de 25 h
**Decisión**: el recordatorio sale con más antelación que el límite de cancelación de 24 h.
**Justificación**: si saliera 24 h antes, el paciente recibiría el link justo cuando ya no puede cancelar solo. Con 48 h, el horario liberado todavía puede reasignarse por lista de espera.

### DD-13 — Esquema versionado con Flyway
**Decisión**: migraciones SQL versionadas. `ddl-auto` queda en `validate`.
**Justificación**: la restricción de exclusión, las extensiones y los índices parciales necesitan SQL explícito. Además, 5 personas modifican el esquema.

### DD-14 — Nombre del producto: Estudio Sonrisa
**Decisión**: el producto se llama **Estudio Sonrisa**.
**Alternativas consideradas**: Sillón Lleno, Hueco, Próximo, Sillonia.
**Riesgos aceptados**:
- Suena a nombre de clínica: puede confundir al paciente en los emails ("Consultorio X vía Estudio Sonrisa"). Se mitiga con una bajada que aclare que es una herramienta de turnos.
- Al ser una frase descriptiva del rubro, el registro de marca puede ser difícil.
**Pendiente**: búsqueda de antecedentes en el INPI y de disponibilidad del dominio en NIC.ar antes del lanzamiento.
**Implementación**: el nombre vive en un único lugar (`design-system/brand.js` y una constante de configuración del backend para los emails) para poder cambiarlo sin tocar cada pantalla.
**Decidido por**: la usuaria, el 2026-11-28.

### DD-15 — Java 17 y Spring Boot 4.2
**Decisión**: el backend usa Java 17 (LTS) y Spring Boot 4.2.0 (Spring Framework 7, Hibernate 7.4, Flyway 12, Jackson 3, Testcontainers 2), generado desde Spring Initializr con Gradle 9.9.2 (wrapper, Groovy DSL).
**Contexto**: al iniciar C-01, la máquina de la responsable tenía JDK 17, y Spring Initializr ya no ofrecía Spring Boot 3.x (solo 4.0, 4.1 y 4.2).
**Alternativas consideradas**: Java 21 (instalar otro JDK en las 5 máquinas); Spring Boot 3.x armado a mano (fuera del soporte de Initializr).
**Justificación**: Java 17 ya está disponible y es LTS soportado por Boot 4. Boot 4.2 es la versión estable con soporte vigente.
**Trade-offs aceptados**: menos ejemplos y documentación de terceros para Boot 4; cambios de Jackson 3 y de los paquetes de test respecto de Boot 3. Todo el equipo usa JDK 17.
**Decidido por**: la usuaria, el 2026-11-28 (change `c-01-backend-scaffolding`).

### DD-16 — Hosting gratuito: Netlify, Render, Neon y cron-job.org
**Decisión**:
- **Frontend**: Netlify, con `_redirects` que reenvía `/api/*` al backend con estado 200 (mismo origen, DD-03) y *fallback* a `index.html`.
- **Backend**: Render, plan Free con Docker (`backend/Dockerfile`).
- **PostgreSQL**: Neon, plan Free (PG 17 con `btree_gist`).
- **Cron externo**: cron-job.org, que envía `POST /api/internal/tick` con `X-Tick-Secret` cada 10 minutos.

**Contexto**: resuelve la parte de hosting de PA-03 con los criterios de `12`. Los límites están relevados el 2026-11-28 y hay que reconfirmarlos en las páginas oficiales antes de cada cambio:

| Servicio | Límite relevado | Consecuencia |
|---|---|---|
| Render Free | 512 MB de RAM, 0,1 CPU, 750 h/mes. Se apaga a los 15 min sin tráfico (arranque en frío cercano a 1 min). **Bloquea el SMTP saliente (25, 465 y 587) desde septiembre de 2025** | JVM con flags de poca memoria. Tick cada 10 min. Email solo por API HTTP |
| Neon Free | 100 CU-h/mes por proyecto. Se apaga a los 5 min de inactividad y despierta en milisegundos. 0,5 a 1 GB por proyecto (las fuentes no coinciden). No vence | Pool chico sin conexiones en reposo. El tick no debe tocar la base sin medir el consumo |
| Netlify Free | 300 minutos de build y 100 GB de transferencia por mes | Alcanza para v1 |
| cron-job.org | Gratuito, `POST` con headers propios y aviso de fallas | — |

**Alternativas consideradas**:
- Cloudflare Pages: sin proxy 200 a dominios externos, salvo con un Worker.
- Koyeb: pide tarjeta desde febrero de 2026.
- Fly.io: ya no tiene plan gratuito, solo una prueba.
- Base gratuita de Render: vence a los 30 días.
- Supabase: se pausa después de 7 días sin actividad y no tiene backups en el plan gratuito.
- Oracle Always Free: siempre encendido, pero pide tarjeta, la capacidad es incierta y el mantenimiento es manual.

**Trade-offs aceptados**: el servicio depende de que el cron funcione para no dormirse; si falla, el paciente sufre un arranque en frío. Es una plataforma por pieza, con cuatro cuentas. El email queda **fuera de esta DD**: Render Free impide el SMTP, así que C-05 tiene que elegir un proveedor con API HTTP (PA-03, parte de email).
**Fuentes**: changelog de Render sobre el bloqueo de SMTP; documentación de Neon sobre `btree_gist`; comparativas de planes gratuitos de 2026 (ver `docs/despliegue.md`).
**Decidido por**: la usuaria, el 2026-11-28 (change `c-03-despliegue-esqueleto`).

---

## Supuestos inferidos

| Código | Supuesto | Origen | Riesgo si es falso | Cómo validar |
|---|---|---|---|---|
| SU-01 | Vite como única herramienta del frontend. ~~Java 21 y Spring Boot 3.x~~: reemplazados por DD-15 | Propuesta en la KB, aceptada | Bajo: cambio de configuración | Confirmar en C-02 |
| SU-02 | 48 h de antelación es un buen valor por defecto para el recordatorio | DD-12 | Medio: menos confirmaciones o recordatorios olvidados | Medir confirmaciones y cancelaciones por antelación |
| SU-03 | El email alcanza para bajar el ausentismo | Discovery (riesgo principal) | **Alto**: el diferencial no funciona | Piloto con 2 o 3 consultorios, medir antes y después (métricas de 01) |
| SU-04 | "Token único por turno" se implementa como un token por oferta y por destinatario | Respuesta de la usuaria, interpretada | Bajo: es un cambio de diseño acotado | Confirmar con la usuaria (IN-01) |
| SU-05 | Un horario ofertado a la lista de espera sigue disponible para la reserva online común | Inferido | Medio: confusión del paciente | Validar en el piloto |
| SU-06 | Sesiones guardadas en la base (Spring Session JDBC) | Reinicios del hosting gratuito | Bajo | Verificar en el hosting elegido |
| SU-07 | Máximo 2 turnos futuros reservados online por DNI y consultorio (RN-RE-03) | Inferido (antiabuso) | Bajo | Confirmar con odontólogos |
| SU-08 | Se rechaza la reserva si el DNI existe y el apellido no coincide (RN-RE-04) | Inferido (protección de datos) | Medio: rechazos por errores de tipeo | Medir rechazos; considerar una comparación tolerante |
| SU-09 | Los turnos sin confirmar no se cancelan solos (RN-TU-02) | Inferido | Medio: horarios ocupados por pacientes que no vienen | Revisar con datos de ausentismo |
| SU-10 | Inicios de turno en pasos de 15 minutos (RN-AG-05) | Inferido | Bajo | Hacerlo configurable si hace falta |
| SU-11 | Límites de tasa de RN-SG-05 y bloqueo de login de RN-AU-05 | Inferido | Bajo | Ajustar con tráfico real |
| SU-12 | DNI argentino de 7 a 9 dígitos (RN-PA-02) | Inferido | Bajo: excluye a pacientes extranjeros | PA-06 |
| SU-13 | Un bloqueo sobre turnos existentes no los cancela automáticamente (RN-AG-06) | Inferido | Bajo | Confirmar con odontólogos |
| SU-14 | Una oferta se envía a un máximo de 20 inscriptos (ver 11) | Inferido (protege la cuota de email) | Bajo | Ajustar con datos reales |

---

## Marco normativo considerado (no es asesoramiento legal)

| Norma | Impacto en el diseño | Estado |
|---|---|---|
| Ley 25.326 (protección de datos personales) | Datos de salud = datos sensibles. El consultorio es responsable de la base y el SaaS es encargado del tratamiento. Implica minimización, seguridad, derechos de acceso y rectificación, y posible inscripción de la base ante la AAIP | Contemplado en el diseño (RN-SG). Falta revisión legal (PA-07) |
| Ley 26.529 (derechos del paciente e historia clínica) | Inalterabilidad, integridad y conservación de la historia clínica | Append-only (DD-10). La política de conservación y de baja de cuenta está pendiente (PA-07, PA-09) |
| Ley 27.706 (historia clínica electrónica) | Posibles requisitos de interoperabilidad o formato | Impacto no evaluado (PA-07) |
