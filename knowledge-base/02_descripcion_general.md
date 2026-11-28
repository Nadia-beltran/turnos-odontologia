# Descripción General

## Stack tecnológico

| Capa | Tecnología | Versión mínima | Notas |
|---|---|---|---|
| Frontend | TypeScript sin framework | TS 5.x | Restricción de la usuaria |
| Build del frontend | Vite | 5.x | **Suposición** (SU-01): única herramienta, sin framework de UI |
| Backend | Spring Boot (Web MVC, Security, Data JPA, Validation, Mail) sobre Spring Framework 7 | 4.2.0 | Restricción de la usuaria; versión decidida en DD-15 |
| Lenguaje del backend | Java | 17 (LTS) | Decisión DD-15 (reemplaza la suposición SU-01) |
| Build del backend | Gradle con Groovy DSL y wrapper | 9.9.2 | Restricción de la usuaria |
| ORM | Hibernate (vía Spring Data JPA) | 7.4 | Restricción de la usuaria |
| Base de datos | PostgreSQL | 15+ | Restricción de la usuaria. Requiere la extensión `btree_gist` (ver 04) |
| Migraciones | Flyway | 12.x | DD-13: el esquema se versiona con migraciones, no con `ddl-auto` |
| Email | Proveedor SMTP o API en plan gratuito | — | Pendiente de elección (ver `11_notificaciones_email.md`) |
| Hosting | Planes gratuitos | — | Ver `12_despliegue_y_entornos.md` |

## Arquitectura general

La API y el frontend se despliegan por separado. El navegador ve un **único origen**: el hosting estático reenvía `/api/*` hacia el backend (DD-03).

```
                    ┌──────────────────────────────────────────┐
 Navegador ───────► │ Hosting estático (frontend TS compilado) │
 (odontólogo,       │   /           → archivos estáticos       │
  paciente)         │   /api/*      → reenvío (proxy) ──────────┼──┐
                    └──────────────────────────────────────────┘  │
                                                                  ▼
                    ┌──────────────────────────────────────────────────┐
                    │ Backend Spring Boot (monolito modular, API REST)  │
                    │  ├─ auth / consultorios / agenda / turnos         │
                    │  ├─ lista de espera / pacientes / ficha clínica   │
                    │  ├─ notificaciones (outbox + tarea programada)    │
                    │  └─ auditoría / reportes / exportación            │
                    └───────────────┬───────────────────┬──────────────┘
                                    ▼                   ▼
                             PostgreSQL          Proveedor de email
                       (esquema compartido,       (SMTP o API)
                        consultorio_id)
```

**Justificación**:

- **Monolito modular en el backend**: con 5 desarrolladores, prioridad en velocidad y presupuesto cero, un único servicio desplegable es lo más barato de operar. Los módulos por dominio permiten repartir el trabajo sin pisarse.
- **Frontend separado con reenvío de `/api`**: permite desplegar el frontend de forma independiente, sin cookies de terceros ni CORS (DD-02, DD-03).
- **Multi-tenant con esquema compartido**: es la única opción viable en un plan gratuito de base de datos (DD-01).

## Integraciones externas

| Servicio | Propósito | Tipo | MVP |
|---|---|---|---|
| Proveedor de email transaccional | Confirmaciones, recordatorios, links de gestión, ofertas de lista de espera | SMTP o API REST | Sí |
| Mercado Pago | Señas | REST y webhook | No (posterior) |
| WhatsApp Business API | Recordatorios | REST y webhook | No (posterior) |
| ARCA | Facturación electrónica | SOAP | No (posterior) |

## API REST (resumen)

Prefijo `/api`. Las rutas públicas viven bajo `/api/public` y no requieren sesión (ver `03_actores_y_roles.md`).

| Recurso | Endpoints principales | Acceso |
|---|---|---|
| Autenticación | `POST /api/auth/registro`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`, `POST /api/auth/verificar-email` | Público o sesión |
| Consultorio | `GET/PUT /api/consultorio`, `GET/PUT /api/consultorio/horarios` | Odontólogo |
| Prestaciones | `GET/POST/PUT/DELETE /api/prestaciones` | Odontólogo |
| Bloqueos | `GET/POST/DELETE /api/bloqueos` | Odontólogo, recepción |
| Agenda | `GET /api/agenda?desde=&hasta=` | Odontólogo, recepción |
| Turnos | `POST /api/turnos`, `PATCH /api/turnos/{id}/estado`, `POST /api/turnos/{id}/reprogramar`, `POST /api/turnos/{id}/cancelar` | Odontólogo, recepción |
| Pacientes | `GET /api/pacientes?q=`, `GET/PUT /api/pacientes/{id}` | Odontólogo, recepción |
| Ficha clínica | `GET /api/pacientes/{id}/odontograma`, `POST /api/pacientes/{id}/odontograma/registros`, `GET/POST /api/pacientes/{id}/evoluciones` | Odontólogo |
| Lista de espera | `GET /api/lista-espera`, `DELETE /api/lista-espera/{id}` | Odontólogo, recepción |
| Reportes | `GET /api/reportes/ausentismo?desde=&hasta=` | Odontólogo |
| Exportación | `GET /api/exportacion` (archivo ZIP con CSV o JSON) | Odontólogo |
| Auditoría | `GET /api/auditoria?desde=&hasta=` | Odontólogo |
| Público: consultorio | `GET /api/public/c/{slug}`, `GET /api/public/c/{slug}/prestaciones`, `GET /api/public/c/{slug}/disponibilidad?prestacion=&desde=&hasta=` | Público |
| Público: reserva | `POST /api/public/c/{slug}/reservas` | Público |
| Público: lista de espera | `POST /api/public/c/{slug}/lista-espera` | Público |
| Público: gestión del turno | `GET /api/public/turnos/{token}`, `POST /api/public/turnos/{token}/confirmar`, `.../cancelar`, `.../reprogramar` | Token |
| Público: oferta de turno liberado | `GET /api/public/ofertas/{token}`, `POST /api/public/ofertas/{token}/aceptar` | Token |
| Administración SaaS | `GET /api/admin/consultorios`, `PATCH /api/admin/consultorios/{id}` | Administrador SaaS |
