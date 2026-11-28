# Análisis competitivo: sistemas odontológicos de turnos y agenda

**Ámbito**: Argentina (prioridad 1), Latinoamérica (prioridad 2) y referencias internacionales (prioridad 3).
**Fecha de consulta de todas las fuentes**: 28 de noviembre de 2026.
**Método**: lectura de sitios oficiales, páginas de precios y centros de ayuda de cada proveedor. No se realizaron demos ni pruebas con cuentas.

## Convenciones y advertencias de método

- **Comprobado**: la función aparece descrita con detalle operativo, en una tabla de planes, en una demo pública, en una captura o *mockup* rotulado, o en un artículo del centro de ayuda.
- **Declarado**: afirmación comercial del proveedor sin demostración visible (cifras de clientes, porcentajes de reducción de ausentismo, cumplimiento normativo, "24/7").
- **No evidenciado**: la función no aparece en las páginas oficiales leídas. Esto **no prueba** que no exista: solo indica que no está demostrada públicamente.
- Los precios en pesos argentinos (ARS) cambian con frecuencia. Valen únicamente para la fecha de consulta.
- Las reseñas de usuarios no se verificaron de forma independiente. Las calificaciones citadas son las que publica el propio proveedor.
- Limitaciones de cobertura (ver Anexo 2): algunos sitios generan su contenido con JavaScript y no se pudieron leer (ZYRA, Reservo, buena parte de OdontoApp). Doctoralia Argentina rechazó la conexión. Ninguno de estos sistemas recibe puntaje sustantivo, y **deben revisarse manualmente**, en especial Doctoralia, por su peso como marketplace de pacientes en Argentina.

---

## A. Tabla comparativa (18 sistemas, ordenados por relevancia para Argentina)

Abreviaturas: **C** = comprobado · **D** = declarado · **NE** = no evidenciado · **+** = complemento pago · **OS** = obras sociales y prepagas · **MP** = Mercado Pago · **GCal** = Google Calendar.

| # | Sistema (país) | Segmento / modalidad | Agenda | Turnos digitales | Automatización | Clínica | Administración | Integraciones | Seguridad / cumplimiento | Precio publicado (28/11/2026) |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **DentalSoft** (AR) | Consultorio y clínica; nube | Diaria, semanal y mensual; multiprofesional; multisucursal; bloqueos y feriados; sobreturnos con **detección de solapamiento** (C). Sillones: NE | Reserva online en 4 pasos con reconocimiento por DNI y filtro por OS; web propia; confirmar o cancelar desde WhatsApp (C, *mockup*); recepcionista IA por WhatsApp en Pro (D) | Recordatorios por WhatsApp y correo (automáticos solo en Pro); análisis de ausentismo; campañas de recuperación (C) | Odontograma FDI con 18 estados e historial; plan de tratamiento; presupuestos aprobables por link; ortodoncia; consentimientos (C). Periodontograma: NE | Caja con cierre diario; MP; señas (Pro); cobertura de OS; liquidaciones; reportes (C). ARCA: NE | GCal, MP, WhatsApp (C). API: NE | Respaldo y exportación a PDF, Excel y CSV (C). Normativa: NE | Gratis (50 turnos/mes por 3 meses); Gestión $30.000/mes; Pro $60.000/mes por profesional; WhatsApp a USD 0,026 por mensaje |
| 2 | **Dentalink** (CL, con módulo AR) | Consultorio, clínica y cadena; nube | Multiprofesional y multicentro (C). Vistas, sillones y solapamientos: NE | Reserva online; turnos por WhatsApp; Contact Center IA que da, confirma, reprograma y cancela turnos (+, C) | Recordatorios y confirmaciones por WhatsApp; tareas de control; correo masivo; NPS (C, según plan) | Historia clínica; **odontograma y periodontograma**; imágenes; presupuestos; análisis de radiografías con IA (+) (C) | Caja y cobros; OS con conciliación (ayuda AR); cálculo de pago a odontólogos; reportes (C). ARCA y MP: NE | API (+); firma de documentos (+); receta electrónica AR (+) (C) | AWS; ISO 27001, HIPAA y respaldos diarios (D); exportación (D) | **No publicado** (cotización). Contact Center IA desde USD 50/mes |
| 3 | **AgendaPro** (LatAm, con oferta AR) | Multirubro con vertical dental; nube y app | Por odontólogo, práctica y **sillón**; duración por práctica (C). Bloqueos y solapamientos: NE | Reserva online 24/7, link, Google Reserve y marketplace; IA en WhatsApp, Instagram y Messenger que agenda, reagenda y cancela (C, *mockup*) | Recordatorios por WhatsApp; recuperación de inasistencias; reactivación de pacientes; campañas (C, plan Pro IA) | Ficha con anamnesis y firma electrónica; presupuestos; tratamientos (C). **Odontograma solo como archivo adjunto**. Periodontograma: NE | Caja multisede; **señas**; MP con cuotas; honorarios; reportes (C). OS: NE. Facturación: "próximamente" | Número de WhatsApp propio, GCal, MP, API a USD 200/mes (C) | Cifrado y respaldos (D); alineación con Leyes 25.326 y 26.529 (D) | $9.900 a $129.900/mes IVA incl.; prueba de 14 días sin tarjeta; extras de WhatsApp, IA y API |
| 4 | **ixi** (AR, generalista) | Profesionales de salud, odontología incluida; web y app | Recurrencia, bloqueos, sobreturnos con huecos calculados, varias sedes (C). Sillones: NE | Link 24/7 con elección de sede y servicio; reprogramar respondiendo el WhatsApp; agente IA con confirmación (C, *mockup*) | Recordatorios por WhatsApp (pago por mensaje) (C). Recall: NE | HCE **firmada e inmutable**; SNOMED CT y CIE-10; recetas con CUIR (C). **Odontograma: NE** | Cobro por MP sin comisión de ixi; pago para confirmar el turno; aranceles por cobertura; reporte "a liquidar" (C) | GCal bidireccional, MP, WhatsApp (C). Declara no usar firma digital certificada | **Auditoría de cambios y de lecturas**; huella criptográfica encadenada; Ley 26.529 (D); insignia del registro de bases de datos de la AAIP | $16.000 + IVA por profesional por mes, **cobrado solo si ese mes registra 10 turnos o más**; WhatsApp $45 + IVA por mensaje |
| 5 | **OCLÜ** (AR/ES) | Consultorio y clínica; web y app iOS/Android | Día, semana y mes; **boxes por plan** (1 a 3); tope de profesionales por plan (C) | Autoagenda por link (+); recordatorios y reprogramación por WhatsApp con botones (+) (C) | Recordatorios por correo; WhatsApp (+) (C). Campañas: solo figura en el menú | Odontograma y periodontograma (este con carga por voz +); visor DICOM (+); formulario del paciente con antecedentes y firma (C) | Tesorería, honorarios, stock, laboratorio y seguros (solo nombres de módulos); facturación ARCA (+, D) | API REST (+), ARCA (+), DICOM (+) (D) | Registro de cambios; permisos por puesto; respaldos (D) | $35.000 a $80.000 + IVA/mes (o USD 25 a 56); complementos sin precio |
| 6 | **DentalTec** (AR) | Odontólogos, círculos, federaciones y OS; web | Turnos por odontólogo con intervalos; secretaria con todas las agendas (C) | **Sin reserva online evidenciada**; confirmación por WhatsApp (C, *mockup*) | Recordatorio por WhatsApp al crear el turno (D) | Historia clínica; odontograma; CIE-10; imágenes (D, con video) | **Validación de prácticas con OS en tiempo real**; liquidaciones; facturación ARCA (todo D) | WhatsApp "oficial"; web services de OS (D) | 10 roles; auditoría; JWT y bcrypt; respaldos (D) | Precio por odontólogo no publicado; paquetes de WhatsApp de USD 9 a 48 |
| 7 | **Odonto.ar** (AR, Córdoba) | Profesional solo a clínica de 10 personas; web | Día, semana y mes; agenda de equipo; **duración por motivo** (C, demo) | Link público de reserva (C, demo); reglas configurables de anticipación, cancelación y reprogramación (D) | Recordatorios: NE | **Odontograma interactivo** con estados y registro de autor y fecha; HC; radiografías; presupuestos; nomenclador (C, demo) | Panel de finanzas (D); cálculo de la parte de la OS y del paciente (C); importación desde Excel | Inicio de sesión con Google | Supabase con cifrado; RLS; roles; cita la Ley 25.326 y a la AAIP (C, política) | USD 29, 49 y 99/mes pagados en ARS por MP; prueba de 30 días sin tarjeta |
| 8 | **Turnito** (AR, generalista) | Centros médicos y clínicas; web | Agenda por profesional; recepción ve todas; turnos recurrentes (C) | Link compartible; paciente sin cuenta; confirmar o cancelar por WhatsApp; **la seña bloquea el horario recién al acreditarse** (C) | Recordatorios por WhatsApp con cupo por plan (C) | Ficha básica (Pro). Odontograma: NE | Cobros por MP, PayPal y transferencia; señas; métricas (C) | MP, PayPal, GCal, Meet; API y MCP solo en Enterprise (C) | NE | **Gratis sin vencimiento** (5 % de comisión); $12.700 a $44.500/mes IVA incl. |
| 9 | **DENTQ** (AR) | Consultorio y clínica; web | Sala de espera en tiempo real; sincronización con GCal (D) | NE | NE | Odontograma FDI de adulto y pediátrico; HC con radiografías (D) | Métricas de recaudación (D) | GCal | Roles (D) | $25.000, $75.000 y $187.000/mes; 30 días de prueba con tarjeta |
| 10 | **OdontoGRAMA** (AR) | Consultorio; nube | Agendas personalizables con control de asistencia (D) | NE | NE | Ficha con imágenes y tratamientos (D) | Caja, cuentas corrientes, facturación a OS y prepagas (D) | NE | Respaldo (D) | "Gratis" sin condiciones claras |
| 11 | **OdontoApp** (AR) | Odontólogos y clínicas; nube | El sitio no cargó su contenido | NE | Recordatorios por WhatsApp (en la política de privacidad) | Odontograma (solo mención) | NE | WhatsApp | Ley 25.326 y cifrado en tránsito (C, política) | NE. **Evidencia insuficiente** |
| 12 | **Doctocliq** (PE) | Odontología y otras especialidades; nube y app | Por consultorio y por doctor; agendas agrupadas (C) | Reserva online en todos los planes; confirmación por WhatsApp; IA "Soyla" 24/7 (+) (C) | Recordatorios por WhatsApp con cupo; campañas (+) (C) | Anamnesis; **odontograma y periodontograma**; presupuesto por fases; consentimiento con firma (C) | Caja y multicajas; comisiones; inventario; laboratorio (C). Facturación sin Argentina | WhatsApp API (+), importación desde GCal (C) | Permisos por rol (C) | Gratis (30 citas/mes); USD 19 a 49/mes. **Argentina no figura entre sus países** |
| 13 | **Dentidesk** (CL) | Clínica dental, universidades y sindicatos; web y app | Duración configurable; feriados que bloquean (C) | Botón de reserva online; confirmar o anular por correo (C). WhatsApp: "próximamente" | Recordatorios de control y cumpleaños por correo (C) | Anamnesis; odontograma; fichas por especialidad; presupuestos detallados; consentimientos (C) | Egresos; morosos; liquidación por producción (C). Facturación solo SII de Chile | SII (Chile) | Permisos (C) | Prueba de 15 días (D); precios NE |
| 14 | **Clinicorp** (BR) | Odontología y franquicias; nube y app | Agenda inteligente; reportes de faltas (C) | Reserva online; *check-in* con QR; agentes IA por WhatsApp (C) | Confirmación por WhatsApp, SMS y correo; alerta de retorno; cobranza (C) | Anamnesis; odontograma; ortodoncia y alineadores; presupuestos; planes; integración con centros de radiología (C) | Caja, Pix, NFS-e, comisiones (C), todo orientado a Brasil | Radiología, certificado digital (C) | LGPD; cifrado (D) | R$ 159,90 a 468,90/mes más implementación. **Sin oferta para Argentina** |
| 15 | **Clinic Cloud** (ES, Doctoralia) | Clínicas médicas, con módulo dental; nube | Multiprofesional y multisede (C) | Reserva 24/7 a través de Doctoralia (C) | Recordatorios por WhatsApp, SMS y correo (D; el SMS figura como extra) | Odontograma y periodontograma (plan Max); notas con IA; consentimientos (C) | Facturación de España (Verifactu); liquidaciones (C) | Doctoralia (C) | RGPD certificado; ISO 27001 (D) | 29 a 79 €/mes + IVA. Sin oferta para Argentina |
| 16 | **Dentally** (UK, Henry Schein One) | Independientes y multisede; nube | Agenda; **lista de espera; relleno de huecos de último momento** (D) | Reserva online en el portal (planes superiores) | Recordatorios y confirmaciones automáticas (D) | *Charting*; notas con IA; imágenes con IA; formularios digitales (D) | Reportes (D) | *Marketplace* de socios; portal para desarrolladores | **AWS, 2FA, restricción por IP, registro de auditoría, 5 roles** (página de seguridad) | £134 a £1.021/mes. Sin oferta para Argentina |
| 17 | **Curve Dental** (US/CA) | Independientes y DSO; nube | Módulo *scheduling* (D) | Reserva online; IA "Quinn" (D) | Recordatorios por SMS y correo; campañas; *recare* (C, tabla de planes) | *Charting*, periodontograma, planes de tratamiento, imágenes, formularios (C, módulos) | Seguros, pagos, membresías (C) | Socios de integración | HIPAA (D); respaldo continuo (D) | No publicado. Solo EE. UU. y Canadá |
| 18 | **Open Dental** (US) | Cualquier tamaño; autoalojado (con nube opcional) | Bloqueos; configuración de agenda (C) | Web Sched para pacientes nuevos y existentes, para recall y **ASAP (relleno de huecos)** (C, tarifas) | eConfirmations, texting, correo masivo (C) | Odontograma gráfico; formularios digitales; análisis de imágenes con IA (+) (C) | Portal de pagos; consultas propias (C) | Cientos de *bridges*; *clearinghouses* (D) | NE | USD 89/mes por sede para "otros países" + eServices |

### Fichas: fortalezas, limitaciones y evidencia de adopción

| Sistema | Fortalezas evidentes | Limitaciones evidentes | Adopción publicada (siempre declarada por el proveedor) |
|---|---|---|---|
| DentalSoft | Es el ciclo de turnos más completo y **mejor demostrado** del mercado local. Precio publicado y plan gratuito. | No muestra facturación ARCA ni periodontograma. Precio por profesional. WhatsApp automático solo en Pro y con costo por mensaje. No migra historias clínicas. | 11 reseñas en Google (5,0) |
| Dentalink | Profundidad clínica (perio, IA sobre radiografías), módulo específico para Argentina con OS, escala regional. | **Sin precios publicados**. Muchas funciones son complementos pagos. ARCA y MP no evidenciados. | +15.000 clientes; +20 países |
| AgendaPro | El mejor canal digital del paciente: IA multicanal, señas, Google Reserve. Precios en ARS publicados. | **Sin odontograma nativo**. Sin OS. Precios contradictorios entre páginas. Las funciones de IA se concentran en el plan más caro. | +30.000 negocios; +1.200 reseñas (4,8) |
| ixi | Diseño de cumplimiento normativo superior (HCE inmutable, auditoría de lecturas, Ley 26.529). Precio alineado al uso real. | No es odontológico: no tiene odontograma ni presupuestos dentales. Solo publica sus páginas de inicio. | NE |
| OCLÜ | Agenda por boxes, API, ARCA, DICOM, carga clínica por voz. | Casi todo lo diferencial son **complementos sin precio publicado**. Muchos módulos aparecen solo como nombre en el menú. | "Clientes en LATAM" sin cifras |
| DentalTec | Foco institucional en OS y federaciones odontológicas: validación y liquidaciones. | Sin reserva online. Precio del software no publicado. Casi todo es declarado. | +2.000 profesionales; +30 instituciones |
| Odonto.ar | Demo pública interactiva. Odontograma con trazabilidad. Seguridad documentada (RLS, cifrado). | Sin recordatorios ni facturación evidenciados. Producto joven: política de septiembre de 2026. | NE |
| Turnito | Reserva sin fricción: sin cuenta y con seña que bloquea el horario. Plan gratuito. | No es odontológico: sin odontograma, caja ni OS. | "N.º 1 de Argentina"; 171 reseñas en Google (4,8) |
| DENTQ | Odontograma pediátrico; sincronización con GCal. | Sin reserva ni recordatorios evidenciados. Prueba con tarjeta. | NE |
| OdontoGRAMA | Caja y facturación a OS. | Sitio desactualizado (© 2023). Sin precios claros. Poca evidencia. | NE |
| OdontoApp | Cita la Ley 25.326 en su política de privacidad. | No fue posible verificar sus funciones. | +100 profesionales |
| Doctocliq | Relación precio/función muy buena; periodontograma y presupuesto por fases en planes baratos. | **No opera en Argentina** (ni facturación ni pagos). | +6.500 profesionales |
| Dentidesk | Ficha clínica muy completa por especialidad. | Recordatorios solo por correo; WhatsApp "próximamente"; orientado a Chile. | NE |
| Clinicorp | Referencia regional en integración con radiología, agentes IA y cobranza. | Exclusivo de Brasil. | +32.000 clínicas en Brasil |
| Clinic Cloud | Cumplimiento normativo certificado; integración con marketplace de pacientes. | Exclusivo de España. Odontograma solo en el plan superior. | +12.000 doctores |
| Dentally | Lista de espera y relleno de huecos; seguridad bien documentada. | Exclusivo del Reino Unido y Commonwealth; precio alto. | Sin cifras |
| Curve Dental | Ciclo clínico completo en la nube. | Exclusivo de EE. UU. y Canadá; sin precios. | +100.000 usuarios; +6.000 sedes |
| Open Dental | Web Sched ASAP y recall; precios totalmente desglosados. | Autoalojado; en inglés; ecosistema estadounidense. | NE |

---

## B. Matriz de puntuación ponderada (0 a 5)

**Pesos**: T = turnos y automatización (25 %) · C = clínica odontológica (20 %) · I = integraciones locales y WhatsApp (15 %) · A = administración, cobros y facturación (15 %) · P = experiencia del paciente (10 %) · S = seguridad, exportación y trazabilidad (10 %) · $ = precio y facilidad de adopción (5 %).

**Criterio**: se puntúa la **evidencia pública**, no el producto real. Lo declarado puntúa menos que lo comprobado y lo no evidenciado puntúa cero o cerca de cero. "I" y "A" se evalúan según su **aplicabilidad en Argentina**: una integración fiscal brasileña no suma en I.

| Sistema | T | C | I | A | P | S | $ | **Total** |
|---|---|---|---|---|---|---|---|---|
| DentalSoft | 5 | 4 | 3 | 4 | 5 | 3 | 4 | **4,10** |
| Dentalink | 4 | 5 | 3 | 4 | 4 | 4 | 1 | **3,90** |
| AgendaPro | 5 | 2 | 4 | 3 | 5 | 3 | 4 | **3,70** |
| Clinicorp (BR) | 5 | 4 | 2 | 3 | 4 | 3 | 2 | **3,60** |
| OCLÜ | 3 | 4 | 4 | 3 | 3 | 3 | 3 | **3,35** |
| Doctocliq (PE) | 4 | 5 | 2 | 2 | 3 | 2 | 5 | **3,35** |
| ixi | 4 | 1 | 3 | 3 | 4 | 5 | 5 | **3,25** |
| Dentally (UK) | 4 | 4 | 1 | 2 | 4 | 5 | 1 | **3,20** |
| Curve Dental (US) | 4 | 5 | 1 | 2 | 3 | 3 | 1 | **3,10** |
| DentalTec | 2 | 3 | 4 | 4 | 2 | 4 | 2 | **3,00** |
| Odonto.ar | 3 | 4 | 1 | 2 | 3 | 4 | 4 | **2,90** |
| Clinic Cloud (ES) | 3 | 4 | 1 | 2 | 3 | 4 | 3 | **2,85** |
| Open Dental (US) | 4 | 4 | 1 | 2 | 3 | 1 | 3 | **2,80** |
| Turnito | 4 | 1 | 3 | 2 | 4 | 1 | 5 | **2,70** |
| Dentidesk (CL) | 3 | 4 | 1 | 2 | 2 | 1 | 2 | **2,40** |
| DENTQ | 1 | 3 | 1 | 1 | 1 | 1 | 4 | **1,55** |
| OdontoGRAMA | 1 | 2 | 0 | 3 | 0 | 1 | 3 | **1,35** |
| OdontoApp | 1 | 1 | 1 | 0 | 0 | 2 | 0 | **0,80** ⚠️ evidencia insuficiente |

**Lectura crítica**:
- Ningún sistema supera 4,1. No hay un líder que cubra todo el mercado argentino.
- Las puntuaciones altas se reparten entre dos perfiles:
  - **Canal del paciente** (AgendaPro, Turnito): muy buenos en turnos, débiles en la parte clínica.
  - **Clínicos** (Dentalink, Doctocliq, Curve): fuertes en clínica, con integraciones locales parciales o caras.
- Los sistemas extranjeros puntúan alto en T y C pero no son opciones reales en Argentina. Sirven como referencia de funciones, no como competencia directa.

---

## C. Análisis competitivo

### C.1 Funcionalidades que ya son estándar de mercado

Las comprueban la mayoría de los sistemas locales relevantes. **No diferencian**: su ausencia descalifica.

- Agenda multiprofesional en la nube, accesible desde el celular.
- Link de reserva online para el paciente.
- Recordatorio de turno por WhatsApp, con confirmación o cancelación desde el mensaje.
- Historia clínica digital con odontograma.
- Presupuestos.
- Caja o registro básico de cobros.
- Reportes de ocupación y recaudación.
- Roles y permisos.
- Prueba gratuita.

### C.2 Diferenciadores reales (comprobados en 1 a 3 productos)

| Diferenciador | Quién lo demuestra | Observación |
|---|---|---|
| Detección automática de solapamientos y sobreturno explícito | DentalSoft; ixi (huecos calculados) | Poco frecuente pese a ser clave en odontología |
| Agenda por **sillón o box** además de por profesional | AgendaPro, OCLÜ | Crítico para clínicas con más profesionales que sillones |
| Duración por prestación (no por profesional) | AgendaPro, Odonto.ar | Evita turnos de 30 minutos genéricos |
| **Seña que bloquea el horario solo al acreditarse** | Turnito; señas también en AgendaPro y DentalSoft Pro | Ataca directamente el ausentismo |
| Presupuesto compartible y **aprobable por link** | DentalSoft | Mejora la conversión de tratamientos |
| IA conversacional en WhatsApp que agenda y reprograma | AgendaPro, Dentalink (+), DentalSoft Pro, ixi | Cada vez más común, pero siempre en el plan caro o como complemento |
| HCE inmutable con auditoría de **lecturas** | ixi | Diseño alineado con la Ley 26.529 |
| Validación de prácticas con OS en tiempo real | DentalTec (declarado) | Alto valor para el prestador; no demostrado |
| Lista de espera con relleno automático de huecos | Dentally, Open Dental (ASAP) | **No evidenciado en ningún producto argentino** |

### C.3 Vacíos frecuentes del mercado argentino

1. **Lista de espera y reaprovechamiento de cancelaciones**: no está evidenciada en ningún sistema argentino. Un turno cancelado hoy queda vacío.
2. **Facturación ARCA nativa y demostrada**: solo aparece como complemento sin precio (OCLÜ) o como afirmación (DentalTec). Dentalink, AgendaPro y DentalSoft no la evidencian.
3. **Turnos, OS y cobro integrados en un mismo flujo**: quien es fuerte en OS (DentalTec) no ofrece reserva online. Quien es fuerte en reserva (AgendaPro, Turnito) no maneja OS.
4. **Agenda por recurso físico** (sillón o box) con duración por prestación: rara, y casi nunca combinada con prevención de solapamientos.
5. **Cumplimiento normativo declarado**: solo ixi nombra la Ley 26.529 y solo Odonto.ar y OdontoApp citan la Ley 25.326. **Ningún producto menciona la Ley 27.706** (historia clínica electrónica).
6. **Costos opacos de WhatsApp**: el costo por mensaje se traslada al consultorio (USD 0,026 en DentalSoft, $45 + IVA en ixi, "lo cobra Meta" en AgendaPro y OCLÜ). El costo final es difícil de prever.
7. **Precios no publicados** en jugadores relevantes (Dentalink, DentalTec y la mayoría de los complementos de OCLÜ).
8. **Migración clínica**: DentalSoft aclara que no migra historias clínicas, odontogramas ni radiografías. La dificultad para cambiar de sistema es alta.
9. **Recall por tratamiento** (por ejemplo, control a los 6 meses tras una limpieza): solo aparece de forma genérica (campañas de reactivación). Casi ninguno lo vincula a la prestación realizada.

### C.4 Oportunidades de innovación

- **Agenda inteligente de recursos**: profesional, sillón y prestación con duración propia, prevención de solapamientos y sobreturno explícito y auditado.
- **Ciclo antiausentismo completo**: recordatorio, confirmación, seña opcional con MP según prestación, y lista de espera que ofrece el hueco liberado al siguiente paciente por WhatsApp.
- **Recall clínico**: controles periódicos generados por la prestación realizada, no por campañas genéricas.
- **Cumplimiento normativo como argumento de venta**: Leyes 25.326, 26.529 y 27.706 documentadas, registro de accesos, exportación completa y portabilidad de datos (reduce el temor a quedar atado al proveedor).
- **Precio transparente con WhatsApp incluido hasta un cupo** y sin complementos ocultos.
- **Autogestión del paciente**: reprogramar o cancelar dentro de reglas de anticipación, sin cuenta (DNI y teléfono).
- **OS en el turno**: plan y coseguro informados al reservar, con la liquidación a OS como etapa posterior.

---

## D. Recomendación final

### D.1 Cinco competidores prioritarios para analizar mediante demo

1. **DentalSoft**: es el estándar local del ciclo de turnos. Conviene verificar en vivo la detección de solapamientos, el reconocimiento por DNI y el flujo de confirmación por WhatsApp.
2. **Dentalink**: líder regional con módulo argentino. Conviene obtener precios reales y ver el funcionamiento de OS, conciliaciones y Contact Center IA.
3. **AgendaPro**: referencia de canal digital y señas. Conviene medir qué incluye cada plan y cuánto pesa en la práctica la falta de odontograma nativo.
4. **OCLÜ**: agenda por boxes, ARCA y API. Conviene comprobar qué existe de verdad detrás de los módulos y cuánto cuestan los complementos.
5. **DentalTec**: referencia en OS y federaciones. Conviene verificar la validación en tiempo real y si existe una forma de reserva online.

*Revisión manual obligatoria fuera de esta lista*: **Doctoralia Argentina** (marketplace de pacientes; el sitio no respondió durante la consulta).

### D.2 Tres productos de referencia para experiencia de usuario

1. **Turnito**: flujo de reserva del paciente sin cuenta y con seña que bloquea el horario.
2. **Odonto.ar**: odontograma interactivo con estados y trazabilidad; tiene demo pública evaluable sin registrarse.
3. **ixi**: agenda con huecos calculados, gestión del turno respondiendo el WhatsApp y transparencia de precio y cumplimiento.

> Advertencia: la experiencia de usuario se evaluó sobre demos públicas y *mockups*. Hay que confirmarla con uso real.

### D.3 MVP sugerido

**Imprescindibles (v1)**:
- Agenda diaria y semanal por profesional **y por sillón**, con duración por prestación, bloqueos y feriados, **prevención de solapamientos** y sobreturno explícito.
- Reserva online por link sin cuenta (DNI y teléfono), con reglas de anticipación.
- Recordatorio por WhatsApp con confirmación o cancelación desde el mensaje, y reprogramación autogestionada.
- Registro de asistencia y ausencias, con reporte de ausentismo.
- Ficha del paciente básica con odontograma (FDI, adulto y pediátrico) e historial de cambios.
- Roles: administrador, odontólogo y recepción, con registro de auditoría de cambios.
- Exportación completa de datos del paciente y de la agenda.

**Diferenciadores (v1 o v1.1)**:
- **Lista de espera** que ofrece automáticamente el turno liberado.
- **Seña opcional con Mercado Pago** por prestación.
- **Recall** generado según la prestación realizada.
- Precio transparente con cupo de WhatsApp incluido.
- Captura de OS y plan al reservar.

**Para etapas posteriores**:
- Facturación electrónica ARCA.
- Liquidación y validación con OS.
- Periodontograma, radiografías y DICOM.
- Presupuestos aprobables por link y planes de tratamiento.
- Caja y liquidación de honorarios.
- Asistente IA conversacional en WhatsApp.
- Multisede.
- Campañas de marketing.
- Telemedicina y API pública.

---

## Anexo 1. Fuentes consultadas (28/11/2026)

| Sistema | URL |
|---|---|
| DentalSoft | https://dentalsoft.com.ar/ |
| Dentalink | https://www.softwaredentalink.com/ar · https://www.softwaredentalink.com/ar/planes · https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina |
| AgendaPro | https://agendapro.com/ · https://agendapro.com/ar/dental/software-odontologico · https://agendapro.com/ar/planes |
| ixi | https://ixi.ar/ |
| OCLÜ | https://www.oclucrm.com/ |
| DentalTec | https://web.dentaltec.com.ar/ · https://web.dentaltec.com.ar/preguntas-frecuentes · https://web.dentaltec.com.ar/funcionalidades |
| Odonto.ar | https://odonto.ar/ · https://odonto.ar/demo · https://odonto.ar/privacidad |
| Turnito | https://turnito.app/ar/app-turnos-medicos/ · https://turnito.app/planes/ · https://turnito.app/blog/las-mejores-apps-de-turnos-para-odontologos-en-argentina-2026/ (blog del proveedor) |
| DENTQ | https://dosnubesmedia.com/dentiq |
| OdontoGRAMA | https://odontograma.com.ar/ |
| OdontoApp | https://odontoapp.com.ar/ · https://odontoapp.com.ar/privacidad |
| Doctocliq | https://www.doctocliq.com/ · https://www.doctocliq.com/planes-y-precios |
| Dentidesk | https://www.dentidesk.com/ · https://www.dentidesk.com/dentidesk/feature |
| Clinicorp | https://www.clinicorp.com/ · https://www.clinicorp.com/planos |
| Clinic Cloud | https://clinic-cloud.com/ · https://clinic-cloud.com/tarifas |
| Dentally | https://dentally.com/ · https://www.dentally.com/en-gb/pricing · https://www.dentally.com/en-gb/security-policy |
| Curve Dental | https://www.curvedental.com/ · https://www.curvedental.com/pricing |
| Open Dental | https://www.opendental.com/ · https://www.opendental.com/site/fees.html · https://www.opendental.com/site/patientportal.html |

## Anexo 2. Sistemas identificados pero no evaluados

| Sistema | Motivo | Acción sugerida |
|---|---|---|
| Doctoralia Argentina / Docplanner | El servidor rechazó la conexión (ECONNREFUSED) | Revisión manual prioritaria |
| ZYRA (appzyra.ar) | El contenido se genera con JavaScript y no se pudo leer | Revisión manual en navegador; según buscadores, ofrece ARCA y WhatsApp |
| Reservo (reservo.cl) | El contenido se genera con JavaScript y no se pudo leer | Revisión manual; declara factura ARCA y MP para Argentina |
| MednIA, Benty, Dentiqa | Mencionados en una guía de Reservo que no se pudo leer; sin URL oficial confirmada | Confirmar existencia y URL |
| DrApp (drapp.la) | Lo menciona el blog de Turnito; no se leyó el sitio oficial | Revisión breve (generalista médico) |
| OdonticLabs (Córdoba) | Aparece en buscadores; no se leyó | Revisión breve |
| Gesden / Infomed (ES) | Conexión rechazada | Baja prioridad (mercado español) |

## Anexo 3. Precios de mensajería WhatsApp trasladados al consultorio

| Sistema | Modelo publicado |
|---|---|
| DentalSoft | USD 0,026 por recordatorio automático (plan Pro) |
| ixi | $45 + IVA por mensaje entregado, opcional |
| DentalTec | Paquetes: USD 9 (50 mensajes), USD 25 (150), USD 48 (300); periodicidad no indicada |
| Turnito | Cupo incluido: 30, 100 o 250 mensajes/mes según el plan |
| AgendaPro, OCLÜ | Meta cobra los mensajes directamente al comercio |
| Doctocliq | Cupo incluido según el plan; el excedente "varía según el país" |
