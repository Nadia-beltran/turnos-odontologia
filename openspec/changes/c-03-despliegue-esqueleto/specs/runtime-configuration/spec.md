# Spec Delta

## Purpose

Garantiza que el backend en producción nunca arranque con configuración incompleta o con valores de desarrollo, y que el error diga exactamente qué falta.

## ADDED Requirements

### Requirement: Variables obligatorias del perfil de producción
Con el perfil `prod` activo, el backend SHALL exigir con valor no vacío estas variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_BASE_URL`, `APP_MAIL_FROM`, `APP_MAIL_DAILY_QUOTA`, `APP_INTERNAL_TICK_SECRET`, `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. Hasta que exista un proveedor de email (C-05), las variables `SPRING_MAIL_*` MUST NOT ser obligatorias.

#### Scenario: Configuración completa sin SMTP
- **WHEN** el backend arranca en `prod` con todas las variables obligatorias y sin ninguna `SPRING_MAIL_*`
- **THEN** la verificación de configuración no falla

#### Scenario: Variable obligatoria faltante
- **WHEN** el backend arranca en `prod` sin `APP_INTERNAL_TICK_SECRET`
- **THEN** el arranque falla antes de conectarse a la base y el mensaje nombra `APP_INTERNAL_TICK_SECRET`

### Requirement: Error que lista todo lo que falta
Cuando faltan varias variables obligatorias, el error de arranque SHALL nombrarlas todas en un único mensaje y MUST NOT incluir el valor de ninguna variable presente.

#### Scenario: Dos variables faltantes
- **WHEN** el backend arranca en `prod` sin `APP_BASE_URL` ni `APP_ADMIN_PASSWORD`
- **THEN** el mensaje de error nombra ambas variables y no muestra valores de otras variables

### Requirement: Sin valores de desarrollo en producción
En el perfil `prod`, la conexión a la base, el secreto del tick y las credenciales del administrador MUST NOT tener valores por defecto en la configuración versionada.

#### Scenario: Perfil prod sin variables de base
- **WHEN** el backend arranca en `prod` sin `SPRING_DATASOURCE_URL`
- **THEN** no usa la base local de desarrollo y el arranque falla nombrando la variable
