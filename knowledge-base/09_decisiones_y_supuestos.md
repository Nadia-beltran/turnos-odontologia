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

---

## Supuestos inferidos

| Código | Supuesto | Origen | Riesgo si es falso | Cómo validar |
|---|---|---|---|---|
| SU-01 | Java 21, Spring Boot 3.x y Vite como única herramienta del frontend | Propuesta en la KB, aceptada | Bajo: cambio de configuración | Confirmar al iniciar el repositorio |
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
