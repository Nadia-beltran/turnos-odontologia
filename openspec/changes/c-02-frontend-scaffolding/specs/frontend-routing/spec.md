# Spec Delta

## Purpose

Define cómo el frontend resuelve las URLs a vistas sin recargar la página, de modo que los enlaces de los emails y las rutas del panel abran directamente la vista correcta.

## ADDED Requirements

### Requirement: Resolución de la URL a una vista
Al cargar la página y en cada navegación, el frontend SHALL mostrar la vista registrada para la ruta actual, incluyendo segmentos variables (por ejemplo `/turno/:token`), dentro del layout que esa ruta declare (público o panel).

#### Scenario: Ruta con segmento variable
- **WHEN** el usuario abre directamente `/r/estudio-ejemplo`, registrada como `/r/:slug`
- **THEN** se muestra la vista de esa ruta con el parámetro `slug` igual a `estudio-ejemplo`

### Requirement: Navegación interna sin recarga
Los enlaces internos SHALL cambiar la vista y la URL sin recargar el documento, y los botones de atrás y adelante del navegador SHALL mostrar la vista correspondiente. Los enlaces a otros orígenes, con `target` o con teclas modificadoras MUST comportarse como enlaces normales.

#### Scenario: Clic en un enlace interno
- **WHEN** el usuario hace clic en un enlace a `/panel`
- **THEN** la URL cambia a `/panel`, se muestra su vista y el documento no se recarga

#### Scenario: Atrás del navegador
- **WHEN** el usuario navegó de `/` a `/panel` y presiona atrás
- **THEN** la URL vuelve a `/` y se muestra su vista

#### Scenario: Ctrl+clic
- **WHEN** el usuario hace Ctrl+clic en un enlace interno
- **THEN** el navegador lo abre en una pestaña nueva y la vista actual no cambia

### Requirement: Ruta inexistente
Para una ruta no registrada, el frontend SHALL mostrar una vista de "página no encontrada" en español, con un enlace al inicio.

#### Scenario: URL desconocida
- **WHEN** el usuario abre `/no-existe`
- **THEN** se muestra la vista de página no encontrada con un enlace a `/`

### Requirement: Fallback del hosting a la aplicación
El hosting estático SHALL servir `index.html` para cualquier ruta que no sea un archivo estático ni empiece con `/api/`, y SHALL reenviar `/api/*` al backend, para que el navegador vea un único origen.

#### Scenario: Recarga en una ruta profunda
- **WHEN** el usuario recarga el navegador en `/panel`
- **THEN** el hosting entrega `index.html` y el router muestra la vista del panel

#### Scenario: Llamada a la API
- **WHEN** el navegador pide `/api/health` al origen del frontend
- **THEN** la request se reenvía al backend y no se responde con `index.html`
