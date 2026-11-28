# Proposal

## Why

DD-01 eligió un esquema compartido entre consultorios: un solo error de filtrado expone los datos de un consultorio a otro. C-04 es el segundo paso del camino crítico (`CHANGES.md`) y fija, antes de que exista cualquier entidad de negocio, el mecanismo que impide esa fuga (RN-MT-01, RN-MT-02) y el registro de auditoría que exige RN-SG-02. Todas las fichas de dominio (C-05 en adelante) heredan las dos cosas.

## What Changes

- Migración `V2__tenancy.sql`: tablas `consultorio`, `usuario` (con el CHECK que liga `rol` y `consultorio_id`) y `sillon`, según `knowledge-base/04`. Las columnas de reglas de reserva de `consultorio` quedan para `V6` (C-08, que las tiene en su scope).
- Migración `V3__auditoria.sql`: tabla `auditoria`, append-only también a nivel de base.
- Infraestructura de tenancy en `ar.turnos.shared`:
  - Una base común para las entidades de negocio, con `consultorio_id`.
  - Un filtro `tenant` de Hibernate habilitado automáticamente en cada sesión.
  - Un contexto de consultorio por hilo de ejecución.
  - Guardas que toman el `consultorio_id` del contexto en cada insert y rechazan modificar o borrar datos de otro consultorio.
- **Modo sistema explícito y acotado**: un bloque declarado en el código que permite leer sin consultorio cuando todavía no se conoce, por ejemplo la búsqueda por email del login de C-06. Fuera de ese bloque, una operación sin consultorio sigue fallando.
- **Sin login todavía**: el contexto de consultorio se establece de forma explícita (tests y servicios). C-06 lo va a poblar desde la sesión autenticada, y las rutas públicas desde el slug o el token. C-04 no implementa autenticación ni abre rutas.
- Servicio de auditoría en `ar.turnos.auditoria` (`registrar(accion, entidad, id, datos)`), que se une a la transacción de quien lo llama. Incluye un punto de extensión para que C-06 aporte el actor autenticado sin editar este módulo.
- Suite de aislamiento reutilizable con dos consultorios (A y B), que extiende `AbstractIntegrationTest` y que cada ficha posterior extiende para sus entidades.

## Capabilities

### New Capabilities
- `tenant-isolation`: aislamiento de los datos de negocio por consultorio, que incluye el esquema base de `consultorio`, `usuario` y `sillon`, la resolución del consultorio solo del lado del servidor y la garantía de que los datos de un consultorio nunca se leen ni se modifican desde otro.
- `audit-log`: registro append-only de las acciones (actor, consultorio, acción, entidad, estado previo, IP y fecha), que no se puede alterar ni borrar.

### Modified Capabilities
<!-- Ninguna: api-errors y schema-migrations no cambian sus requisitos. V2 y V3 se rigen por la spec schema-migrations vigente. -->

## Impact

- **Código nuevo** (zona de C-04):
  - `backend/src/main/java/ar/turnos/shared/tenancy/`
  - `backend/src/main/java/ar/turnos/auditoria/`
  - `backend/src/main/resources/db/migration/V2__tenancy.sql` y `V3__auditoria.sql`
  - En los tests: `backend/src/test/java/ar/turnos/shared/tenancy/`, `backend/src/test/java/ar/turnos/auditoria/` y una migración solo de test en `backend/src/test/resources/db/testmigration/`.
- **Se modifica**: `AbstractIntegrationTest` (módulo `shared`, tests), para sumar la ubicación de las migraciones de test sin crear un segundo contexto de Spring.
- **No se toca**: `V1__extensiones.sql`, `SecurityConfig` ni los paquetes `auth`, `consultorio` y demás módulos. Las entidades JPA `Consultorio`, `Usuario` y `Sillon` las crea C-06, que tiene esos paquetes en su zona.
- **Dependencias**: ninguna nueva. Se usa el soporte de filtros de Hibernate 7.4 (incluido en Spring Boot 4.2.0).
- **Desbloquea**: C-05 (outbox) y, a través de C-06, todo el dominio.
