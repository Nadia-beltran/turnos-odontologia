# Preguntas Abiertas

## Inconsistencias detectadas

### IN-01 — Token de la lista de espera: ¿uno por turno o uno por destinatario?
**La usuaria dijo**: "se avisa a todos los que esperan con un token único por turno, y el primero que acepta se lo queda".
**La KB modela**: un token por oferta y por destinatario (`oferta_envio`, SU-04).
**Impacto**: con un solo token compartido no se puede saber quién aceptó, y cualquiera que reciba un reenvío podría tomar el turno.
**Resolución propuesta**: mantener un token por destinatario. Sigue siendo "único por turno" en el sentido de que no sirve para otro turno. Confirmar con la usuaria.

### IN-02 — Despliegue separado con sesión por cookie (resuelta)
**Decisión A**: API y frontend separados (DD-02).
**Decisión B**: sesión con cookie (DD-11).
**Impacto**: con dominios distintos, la cookie es de terceros y se bloquea.
**Resolución**: reenvío de `/api` desde el hosting estático (DD-03). Aprobado por la usuaria el 2026-11-28.

### IN-03 — Plan pago sin cobro automático
**El discovery dice**: plan gratuito con límites y plan pago.
**El alcance v1 dice**: el cobro automático de suscripciones queda fuera.
**Impacto**: no hay forma de cobrar en el MVP salvo de manera manual.
**Resolución propuesta**: lanzar con plan gratuito y cambio de plan manual por `ADMIN_SAAS` (RN-PL-02). Decidir el cobro en PA-02.

## Preguntas abiertas (priorizadas)

| Código | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| PA-03 | **Alta** (resuelta en parte) | ~~Hosting gratuito de frontend, backend y PostgreSQL~~: resuelto por **DD-16** (Netlify, Render, Neon y cron-job.org). **Pendiente**: proveedor de email. Tiene que tener **API HTTP**, porque Render Free bloquea el SMTP saliente, y cumplir los criterios de 11 | C-05 (outbox de emails) | Equipo técnico |
| PA-07 | **Alta** | Revisión legal: rol de encargado del tratamiento (Ley 25.326) y contrato con el consultorio, inscripción ante la AAIP, plazo de conservación de la historia clínica (Ley 26.529) e impacto de la Ley 27.706 | Lanzamiento con datos reales | Product owner y asesoría legal |
| PA-08 | **Alta** | ¿Hay presupuesto para un dominio propio? Sin dominio no se pueden configurar SPF, DKIM y DMARC, y los emails tienen más probabilidad de caer en spam, lo que afecta al diferencial principal | Lanzamiento | Product owner |
| PA-01 | **Alta** | Límites del plan gratuito (turnos por mes, emails) y precio del plan pago. Deben ser coherentes con la cuota diaria de email (ver 11) | Reserva online en producción | Product owner |
| PA-04 | Media | ¿El rol `RECEPCION` entra en el MVP? (US-005) | Planificación del sprint de usuarios | Product owner |
| PA-05 | Media | Catálogo final de estados del odontograma y si se distingue "existente", "a realizar" y "realizado" (propuesta en 04) | Épica 7 | Odontólogo de referencia |
| PA-02 | Media | ¿El cobro de la suscripción entra en el MVP o se lanza solo con plan gratuito? | Modelo comercial | Product owner |
| PA-09 | Media | ¿Qué pasa con los datos cuando un consultorio se da de baja? (exportación obligatoria, plazo de conservación, borrado) | Baja de cuentas | Product owner y asesoría legal |
| PA-12 | ~~Media~~ Resuelta | Nombre del producto: **Estudio Sonrisa** (DD-14). Pendiente: verificar marca en el INPI y dominio en NIC.ar | — | Product owner |
| PA-10 | Baja | ¿Se agrega un CAPTCHA gratuito (por ejemplo, Cloudflare Turnstile) en la reserva y la inscripción, además del límite de tasa? | Si aparece abuso | Equipo técnico |
| PA-06 | Baja | ¿Cómo se registran pacientes extranjeros sin DNI? | — | Product owner |
| PA-11 | Baja | Revisión manual de Doctoralia Argentina, ZYRA y Reservo (no se pudieron leer en el discovery) | — | Product owner |
| PA-13 | Baja | Copias de seguridad: ¿alcanzan las del plan gratuito de la base, o se suma un `pg_dump` cifrado periódico? | Antes de tener datos reales | Equipo técnico |
