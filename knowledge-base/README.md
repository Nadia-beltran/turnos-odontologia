# Turnos Odontología: Base de Conocimiento

Base de conocimiento armada en modo entrevista a partir del discovery (`discovery/discovery.md`) y del análisis competitivo (`discovery/research/analisis-competitivo.md`). Fecha: 2026-11-28.

## Índice de archivos

| Archivo | Contenido |
|---|---|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, objetivos por actor, alcance v1, fuera de alcance, métricas |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack, arquitectura general, integraciones, resumen de la API REST |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Actores, matriz de permisos y rutas públicas |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Entidades, diagrama de relaciones, restricción de exclusión y seed |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | Reglas codificadas RN-XX-NN por dominio |
| [06_funcionalidades.md](06_funcionalidades.md) | 9 épicas con historias US-NNN y criterios de aceptación |
| [07_flujos_principales.md](07_flujos_principales.md) | Registro, reserva, gestión del turno, lista de espera, asistencia, outbox |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, estructura de directorios, multi-tenancy, seguridad, tests, variables de entorno |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Decisiones DD-NN, supuestos SU-NN y marco normativo |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias IN-NN y preguntas PA-NN priorizadas |
| [11_notificaciones_email.md](11_notificaciones_email.md) | Tipos de email, outbox, cuota diaria, capacidad y entregabilidad |
| [12_despliegue_y_entornos.md](12_despliegue_y_entornos.md) | Entornos, requisitos del hosting gratuito, *keep-alive*, CI y copias de seguridad |

## Guía rápida para desarrolladores

1. Entender el dominio → [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md)
2. Entender los datos → [04](04_modelo_de_datos.md)
3. Entender las reglas → [05](05_reglas_de_negocio.md)
4. Entender la arquitectura → [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md), [12](12_despliegue_y_entornos.md)
5. Implementar → [07](07_flujos_principales.md), [06](06_funcionalidades.md), [11](11_notificaciones_email.md)
6. Antes de codificar → [10](10_preguntas_abiertas.md)

## Resumen ejecutivo

SaaS multi-tenant para odontólogos independientes con un sillón, que ataca el **ausentismo y los turnos perdidos**:

- Reserva online sin cuenta.
- Gestión del turno por email, con cancelación y reprogramación hasta 24 h antes.
- Una **lista de espera** que ofrece automáticamente cada horario liberado: el primero que acepta se lo queda.

Ese diferencial no está evidenciado en ningún competidor argentino.

**Stack**: Spring Boot, REST, Gradle, Hibernate, PostgreSQL y TypeScript sin framework. Presupuesto cero.

**Riesgos principales**:
- La efectividad del email como único canal.
- Los límites del email y del hosting gratuitos.
- El cumplimiento normativo con datos de salud.
