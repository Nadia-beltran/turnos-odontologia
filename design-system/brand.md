# Marca: Estudio Sonrisa

## Nombre

- **Nombre**: Estudio Sonrisa. Fuente: decisión de la usuaria, DD-14 en `knowledge-base/09_decisiones_y_supuestos.md`.
- **Tagline (provisoria)**: "Turnos para tu consultorio, sin huecos vacíos". Fuente: propuesta en esta fase, (TBD) hasta que se apruebe.
- **Uso en emails al paciente**: "{Nombre del consultorio} vía Estudio Sonrisa". El consultorio va primero, para que el paciente no confunda el producto con otra clínica (riesgo señalado en DD-14).
- **Pendiente**: búsqueda de antecedentes en el INPI y disponibilidad de dominio en NIC.ar.

## Tono de voz

- **Español rioplatense**, con voseo: "Elegí tu horario", "Te esperamos".
- **Cálido y claro**, sin tecnicismos ni jerga clínica con el paciente.
- **Directo en los errores**: qué pasó y qué hacer ("Ese horario ya no está disponible. Elegí otro").
- **Sin culpa ni amenazas** con las ausencias: se registran sin penalidad (RN-AS-03).
- **Con el odontólogo**: profesional y breve; los números van con contexto ("8 % de ausentismo, 6 puntos menos que en octubre").

## Logo

- **Estado**: (TBD). `assets/logo.svg` es un **marcador provisorio**: isotipo de cuadrado redondeado con una sonrisa, más un rótulo visible "LOGO PROVISORIO". No es una marca aprobada.
- **Reglas para el definitivo**:
  - SVG con fondo transparente.
  - Una versión con isotipo y nombre, y otra solo isotipo (para el favicon y el encabezado del celular).
  - Legible a 24 px.
  - Contraste AA sobre `--surface-card` y sobre `--color-primary-600`.

## Color de marca

- Acción y marca: `--color-primary-600` (#12806f), un verde azulado que transmite salud y calma sin el azul frío de hospital.
- Acento: `--color-accent-500` (#f07b5f), coral cálido, la "sonrisa". Solo decorativo o con texto oscuro. Para texto de acento se usa `--color-accent-700`.
- Valores completos en `tokens.css`.

## Imágenes

- **v1 sin fotografías**. Si más adelante se suman: personas reales en consultorio, luz natural, nada de bancos de imágenes con sonrisas exageradas ni dientes en primer plano.
- Íconos de trazo simple y consistentes. La fuente de íconos queda (TBD): se recomienda SVG inline propio, sin librerías remotas.
- Nunca se usan imágenes con datos clínicos o radiografías reales en material de marca.
