# Spec Delta

## Purpose

Garantiza que todo email del sistema se encole junto con el evento que lo origina y se envíe después de forma confiable, priorizada y dentro de la cuota diaria del proveedor (DD-09, RN-NO-01 a RN-NO-04), sin perder emails si el proveedor falla.

## ADDED Requirements

### Requirement: Encolado transaccional
Un email SHALL encolarse como una notificación `PENDIENTE` dentro de la misma transacción del evento que lo origina, y nunca enviarse directamente desde esa operación. Si la transacción se revierte, la notificación MUST NOT existir.

#### Scenario: Evento confirmado
- **GIVEN** (DADO) una operación en el contexto del consultorio ficticio A
- **WHEN** (CUANDO) encola un email `RESERVA_CONFIRMADA` para `paciente@example.test` y su transacción se confirma
- **THEN** (ENTONCES) existe una notificación `PENDIENTE` del consultorio A, con 0 intentos y prioridad 2, y todavía no se envió ningún email

#### Scenario: Evento revertido
- **GIVEN** (DADO) una operación que encola un email y después falla
- **WHEN** (CUANDO) su transacción se revierte
- **THEN** (ENTONCES) no queda ninguna notificación de esa operación

#### Scenario: Sin fecha programada
- **WHEN** (CUANDO) se encola un email sin indicar cuándo enviarlo
- **THEN** (ENTONCES) queda programado para el momento del encolado, en UTC

### Requirement: Datos de encolado válidos
El encolado MUST rechazarse, sin guardar nada, si el destinatario no es un email válido de hasta 254 caracteres, si falta el tipo o el contenido, o si el tipo no tiene sus plantillas HTML y de texto plano.

#### Scenario: Destinatario inválido
- **WHEN** (CUANDO) se encola un email para `no-es-un-email`
- **THEN** (ENTONCES) el encolado se rechaza y no se guarda ninguna notificación

#### Scenario: Tipo sin plantillas
- **GIVEN** (DADO) un tipo de email que no tiene plantillas
- **WHEN** (CUANDO) se lo encola
- **THEN** (ENTONCES) el encolado se rechaza con un error que nombra el tipo

### Requirement: Notificación aislada por consultorio
Una notificación SHALL tomar el consultorio del contexto del servidor, nunca de un parámetro, y MUST NOT ser visible desde otro consultorio. Solo un proceso en modo sistema MAY encolar una notificación sin consultorio (por ejemplo, para el administrador SaaS).

#### Scenario: Listado propio
- **GIVEN** (DADO) una notificación del consultorio A y otra del consultorio B
- **WHEN** (CUANDO) se listan las notificaciones en el contexto de A
- **THEN** (ENTONCES) solo aparece la de A

#### Scenario: Encolado sin contexto
- **WHEN** (CUANDO) se encola un email sin consultorio en el contexto y fuera del modo sistema
- **THEN** (ENTONCES) el encolado se rechaza y no se guarda nada

#### Scenario: Encolado en modo sistema
- **WHEN** (CUANDO) un proceso en modo sistema encola un email
- **THEN** (ENTONCES) la notificación queda sin consultorio

### Requirement: Envío de notificaciones vencidas
El procesamiento SHALL enviar cada notificación `PENDIENTE` con `programada_para` cumplida y marcarla `ENVIADA` con la fecha de envío en UTC. Las notificaciones programadas para el futuro MUST quedar `PENDIENTE`.

#### Scenario: Envío exitoso
- **GIVEN** (DADO) una notificación `PENDIENTE` vencida del consultorio A
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) el servidor SMTP recibe un email para su destinatario y la notificación queda `ENVIADA` con su fecha de envío

#### Scenario: Notificación futura
- **GIVEN** (DADO) una notificación programada para dentro de una hora
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) no se envía y sigue `PENDIENTE`

### Requirement: Orden por prioridad
El procesamiento SHALL enviar las notificaciones vencidas por prioridad ascendente (1 a 4, según el tipo, KB 11) y, a igual prioridad, por `programada_para` ascendente.

#### Scenario: Prioridad antes que antigüedad
- **GIVEN** (DADO) una notificación de prioridad 4 programada hace una hora y otra de prioridad 1 programada hace un minuto
- **AND** (Y) una cuota diaria a la que le queda un solo envío
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) se envía la de prioridad 1 y la de prioridad 4 sigue `PENDIENTE`

### Requirement: Cuota diaria de envíos
El procesamiento MUST NOT superar la cuota diaria configurada. Desde el 80 % de la cuota SHALL enviar solo las prioridades 1 y 2, y al 100 % MUST dejar todo en `PENDIENTE`, sin contar intentos, hasta el día siguiente.

#### Scenario: Cuota agotada
- **GIVEN** (DADO) una cuota de 10 emails, 10 enviados hoy y una notificación de prioridad 1 vencida
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) no se envía nada y la notificación sigue `PENDIENTE` con 0 intentos

#### Scenario: Umbral del 80 %
- **GIVEN** (DADO) una cuota de 10 emails, 8 enviados hoy y una notificación vencida de prioridad 2 y otra de prioridad 3
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) se envía la de prioridad 2 y la de prioridad 3 sigue `PENDIENTE` con 0 intentos

#### Scenario: Cuota que se agota en el lote
- **GIVEN** (DADO) una cuota de 10 emails, 9 enviados hoy y dos notificaciones vencidas de prioridad 1
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) se envía una sola y la otra sigue `PENDIENTE`

#### Scenario: Día nuevo
- **GIVEN** (DADO) una cuota agotada con envíos del día anterior
- **WHEN** (CUANDO) corre el procesamiento en un día nuevo
- **THEN** (ENTONCES) la cuota vuelve a estar disponible y se envían las pendientes

### Requirement: Reintentos con espera creciente
Un envío fallido SHALL incrementar los intentos, guardar el último error y reprogramarse a los 5, 15 y 60 minutos tras el primer, segundo y tercer fallo. Tras el cuarto fallo la notificación MUST quedar `FALLIDA` y no volver a procesarse.

#### Scenario: Primer fallo
- **GIVEN** (DADO) un servidor de email que rechaza el envío
- **WHEN** (CUANDO) corre el procesamiento sobre una notificación con 0 intentos
- **THEN** (ENTONCES) queda `PENDIENTE` con 1 intento, el último error guardado y programada 5 minutos más tarde

#### Scenario: Reintento exitoso
- **GIVEN** (DADO) una notificación con 1 intento fallido, ya vencida, y un servidor de email que funciona
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) queda `ENVIADA`

#### Scenario: Fallo definitivo
- **GIVEN** (DADO) una notificación con 3 intentos fallidos, vencida, y un servidor que rechaza el envío
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) queda `FALLIDA` con 4 intentos y no se envía en procesamientos posteriores

#### Scenario: Proveedor caído
- **GIVEN** (DADO) varias notificaciones vencidas y un servidor de email inalcanzable
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) ninguna se pierde: todas siguen en la base, `PENDIENTE` y reprogramadas

### Requirement: Consultorio suspendido sin envíos
El procesamiento MUST NOT enviar notificaciones de un consultorio `SUSPENDIDO` (RN-MT-03): quedan `PENDIENTE` sin contar intentos. Un consultorio `PENDIENTE_VERIFICACION` SHALL recibir sus emails.

#### Scenario: Consultorio suspendido
- **GIVEN** (DADO) una notificación vencida de un consultorio `SUSPENDIDO`
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) no se envía y sigue `PENDIENTE` con 0 intentos

#### Scenario: Consultorio pendiente de verificación
- **GIVEN** (DADO) una notificación vencida de un consultorio `PENDIENTE_VERIFICACION`
- **WHEN** (CUANDO) corre el procesamiento
- **THEN** (ENTONCES) se envía

### Requirement: Procesamiento sin envíos duplicados
Dos procesamientos simultáneos MUST NOT enviar la misma notificación ni superar juntos la cuota diaria.

#### Scenario: Procesamientos simultáneos
- **GIVEN** (DADO) una notificación vencida
- **WHEN** (CUANDO) dos procesamientos corren al mismo tiempo
- **THEN** (ENTONCES) el servidor SMTP recibe un solo email y la notificación queda `ENVIADA`

### Requirement: Contenido HTML y texto plano
Cada email SHALL enviarse con una versión HTML y otra de texto plano, generadas desde plantillas con el contenido de la notificación como variables, con los valores escapados en HTML y sin imágenes remotas.

#### Scenario: Email multiparte
- **WHEN** (CUANDO) se envía una notificación cuyo contenido incluye `<b>Ana</b>`
- **THEN** (ENTONCES) el email recibido tiene una parte HTML con `&lt;b&gt;Ana&lt;/b&gt;` y una parte de texto plano con el mismo dato

### Requirement: Remitente del consultorio
El remitente visible SHALL ser "{nombre del consultorio} vía {nombre del producto}", con la dirección configurada del sistema, y la respuesta (`Reply-To`) SHALL ir al email de contacto del consultorio. Sin consultorio, el remitente es solo el nombre del producto, sin `Reply-To`.

#### Scenario: Email de un consultorio
- **GIVEN** (DADO) el consultorio ficticio "Consultorio Prueba" con email de contacto `contacto@example.test`
- **WHEN** (CUANDO) se envía una de sus notificaciones
- **THEN** (ENTONCES) el remitente visible es "Consultorio Prueba vía Estudio Sonrisa" y el `Reply-To` es `contacto@example.test`

#### Scenario: Email del sistema
- **WHEN** (CUANDO) se envía una notificación sin consultorio
- **THEN** (ENTONCES) el remitente visible es "Estudio Sonrisa" y no hay `Reply-To`

### Requirement: Sin datos personales en los logs del envío
Los logs del procesamiento MUST registrar solo identificadores y contadores: nunca el destinatario, el contenido ni el texto del error del proveedor (RN-SG-06).

#### Scenario: Envío fallido registrado
- **WHEN** (CUANDO) falla el envío a `paciente@example.test` con un contenido ficticio
- **THEN** (ENTONCES) el log contiene el id de la notificación y no contiene ni el destinatario ni el contenido
