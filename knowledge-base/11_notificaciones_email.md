# Notificaciones por Email

El email es el único canal con el paciente en v1 (DD-06). De él depende el diferencial del producto, así que la **entregabilidad** y la **cuota diaria** son riesgos de primer orden.

## Tipos de email

| Tipo | Disparador | Destinatario | Contenido clave | Prioridad de envío |
|---|---|---|---|---|
| `VERIFICACION_EMAIL` | Registro, o pedido de reenvío | Odontólogo | Link de verificación (vence a las 48 h) | 1 |
| `OFERTA_HUECO` | Turno liberado con inscriptos compatibles | Pacientes en lista de espera | Fecha, hora y prestación; link `/o/{token}`; link de baja | 1 (sensible al tiempo) |
| `RESERVA_CONFIRMADA` | Reserva online, carga manual o aceptación de oferta | Paciente | Datos del turno, dirección del consultorio y link `/t/{token}` | 2 |
| `TURNO_CANCELADO` | Cancelación (por el paciente o el consultorio) | Paciente | Confirmación de la cancelación y link para volver a reservar | 2 |
| `TURNO_REPROGRAMADO` | Reprogramación | Paciente | Datos del turno nuevo y nuevo link `/t/{token}` | 2 |
| `RECORDATORIO` | `inicio − horas_recordatorio` | Paciente | Datos del turno, botón "Confirmar" y link de gestión. Aclara hasta cuándo se puede cancelar | 3 |
| `INSCRIPCION_ESPERA` | Inscripción en la lista | Paciente | Confirmación del rango de fechas y link de baja | 4 |
| Recuperación de contraseña | Pedido del usuario | Usuario | Link de un solo uso (vence en 1 h) | 1 |

## Reglas de contenido

- Español rioplatense, tono cordial, sin datos clínicos (RN-SG-06).
- Cada email tiene versión HTML **y** texto plano.
- El remitente visible es el nombre del consultorio ("Consultorio X vía <producto>"). La respuesta (`Reply-To`) va al email del consultorio.
- Los links se arman con `APP_BASE_URL` y siempre apuntan al frontend, nunca directamente a la API.
- Los emails a pacientes no llevan imágenes remotas ni píxeles de seguimiento.
- Plantillas en `backend/src/main/resources/templates/email/`, con variables, no concatenando texto.

## Envío (outbox)

Ver Flujo 6 en `07_flujos_principales.md`.

- La tarea programada corre cada 5 minutos y procesa por **prioridad** y luego por `programada_para`.
- Respeta `APP_MAIL_DAILY_QUOTA`: al llegar al 80 % de la cuota diaria solo envía las prioridades 1 y 2, y al 100 % deja todo en `PENDIENTE` hasta el día siguiente.
- Reintentos: 3 como máximo, a los 5, 15 y 60 minutos.
- Antes de enviar un recordatorio u oferta se vuelve a verificar el estado: no se recuerda un turno cancelado ni se oferta un horario ya tomado.
- Cada oferta se envía a un **máximo de 20 inscriptos**, en orden de antigüedad de la inscripción (SU-14), para no agotar la cuota con un solo horario.

## Estimación de capacidad

Supuestos por turno: 1 confirmación + 1 recordatorio + 0,2 cancelaciones o reprogramaciones + ofertas a la lista de espera.

| Escenario | Emails por día |
|---|---|
| 1 consultorio con 12 turnos/día | ≈ 30 a 40 |
| Cuota diaria de 300 emails | ≈ 8 consultorios activos |

**Conclusión**: con una cuota gratuita del orden de 300 emails por día, el sistema sostiene **menos de 10 consultorios activos**. El límite del plan gratuito (PA-01) y la elección del proveedor (PA-03) tienen que definirse en conjunto, y el plan pago debería costear un plan de email mayor.

## Proveedor de email (pendiente, PA-03, se decide en C-05)

> El backend corre en Render Free (DD-16), que bloquea el SMTP saliente (puertos 25, 465 y 587). El proveedor tiene que permitir el envío por **API HTTP**. Spring Mail por SMTP no funciona en ese plan.

Criterios de elección:

1. Plan gratuito permanente, sin vencimiento.
2. Cuota diaria suficiente.
3. SMTP o API compatible con Spring Mail.
4. Soporte de SPF, DKIM y DMARC con dominio propio.
5. Webhook o reporte de rebotes.

> Los proveedores con plan gratuito y sus cuotas cambian con frecuencia. Los valores se verifican el día de la elección y se registran en `09_decisiones_y_supuestos.md` como una nueva DD.

## Entregabilidad

- **Sin dominio propio** (PA-08), los emails salen desde un dominio compartido del proveedor y tienen más probabilidad de caer en spam. Es el principal riesgo técnico del diferencial.
- **Con dominio propio**: configurar SPF, DKIM y DMARC (`p=none` al inicio) antes del primer envío real.
- Asunto claro y sin palabras gatillo. Se procesan los rebotes: tras un rebote permanente se marca el email del paciente como inválido y se avisa al consultorio en la ficha de contacto.
