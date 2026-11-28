# Actores y Roles

## Actores del sistema

| Actor | Descripción | Cómo interactúa |
|---|---|---|
| **Odontólogo** (`ODONTOLOGO`) | Dueño del consultorio y cliente del SaaS. Un consultorio tiene exactamente un odontólogo en v1. | Panel web con sesión (cookie) |
| **Recepción** (`RECEPCION`) | Persona que opera la agenda en nombre del odontólogo. **Su inclusión en el MVP está pendiente** (PA-04). Se modela desde el inicio para no rediseñar el control de acceso después. | Panel web con sesión, con permisos reducidos |
| **Paciente** | Persona que reserva un turno. **No tiene cuenta**: se identifica por DNI dentro de cada consultorio. | Página pública del consultorio y links con token recibidos por email |
| **Administrador SaaS** (`ADMIN_SAAS`) | Equipo del producto. No pertenece a ningún consultorio. | Panel de administración con sesión |
| **Sistema** | Tareas programadas: envío de recordatorios, procesamiento de la cola de emails, vencimiento de tokens y ofertas. | Interno, sin interfaz de usuario |

## RBAC: matriz de permisos

C = crear · R = leer · U = actualizar · D = eliminar · — = sin acceso. Todo acceso de `ODONTOLOGO` y `RECEPCION` queda **restringido a su propio consultorio** (RN-MT-01).

| Recurso | ODONTOLOGO | RECEPCION | ADMIN_SAAS | Paciente (token) |
|---|---|---|---|---|
| Consultorio (datos, slug, horarios) | R U | R | R U (estado y plan) | R (datos públicos) |
| Usuarios del consultorio | C R U D | — | R | — |
| Prestaciones | C R U D | R | — | R (solo las reservables online) |
| Bloqueos y feriados | C R U D | C R U D | — | — |
| Agenda y turnos | C R U | C R U | — | R U (solo su turno: confirmar, cancelar, reprogramar) |
| Sobreturno | C | C | — | — |
| Registro de asistencia | U | U | — | — |
| Pacientes (datos de contacto) | C R U | C R U | — | C (al reservar) |
| Ficha clínica (odontograma, evoluciones) | C R | — | — | — |
| Lista de espera | R D | R D | — | C (inscripción), U (aceptar oferta) |
| Reportes | R | — | R (agregados, sin datos de pacientes) | — |
| Exportación de datos | R | — | — | — |
| Auditoría | R | — | R | — |

**Notas**:

- La ficha clínica es **append-only**: no hay U ni D. Una corrección se registra como un nuevo registro que reemplaza al anterior (RN-CL-01).
- Los turnos no se eliminan: se cancelan y mantienen su historial (RN-TU-08).
- `ADMIN_SAAS` **no accede** a datos clínicos ni de contacto de pacientes (RN-SG-03).

## Rutas públicas

Accesibles sin sesión. Todas tienen límite de tasa de solicitudes (*rate limiting*, RN-SG-05).

| Ruta (frontend) | API | Propósito |
|---|---|---|
| `/` | — | Landing del producto |
| `/registro`, `/login`, `/verificar-email` | `/api/auth/*` | Alta de un consultorio e inicio de sesión |
| `/c/{slug}` | `/api/public/c/{slug}/*` | Página de reserva del consultorio |
| `/c/{slug}/lista-espera` | `/api/public/c/{slug}/lista-espera` | Inscripción en la lista de espera |
| `/t/{token}` | `/api/public/turnos/{token}/*` | Gestión del turno por el paciente (confirmar, cancelar, reprogramar) |
| `/o/{token}` | `/api/public/ofertas/{token}/*` | Aceptar un turno liberado |
