# Arquitectura Propuesta

## Patrones aplicados

| Patrón | Dónde se usa | Por qué |
|---|---|---|
| Monolito modular (paquetes por dominio) | Backend | Un solo despliegue en plan gratuito. 5 personas trabajan en módulos separados sin pisarse |
| Multi-tenant con esquema compartido y filtro de Hibernate | Persistencia | Aislamiento por consultorio sin costo de infraestructura (DD-01) |
| Restricción de exclusión en PostgreSQL | Tabla `turno` | Garantía fuerte contra solapamientos y carreras, que no depende del código (DD-04) |
| Bloqueo optimista (`@Version`) | `turno`, `oferta_hueco` | Resuelve "el primero que acepta se lo queda" sin bloqueos largos |
| Outbox transaccional | `notificacion` y tarea programada | Ningún email se pierde si la transacción falla o el proveedor está caído |
| Eventos de dominio después del commit (`@TransactionalEventListener`) | Turno liberado → lista de espera | Desacopla turnos de lista de espera y evita ofertar un horario cuya transacción no se confirmó |
| Tokens opacos con hash | Links de email | El paciente opera sin cuenta y una filtración de la base no expone los links |
| ProblemDetail (RFC 9457) | Errores de la API | Formato de error uniforme para el frontend |
| Mismo origen mediante proxy | Hosting estático → `/api` | Cookie de sesión de primera parte, sin CORS (DD-03) |

## Estructura de directorios

Monorepo con dos aplicaciones.

```
turnos-odontologia/
├── backend/                         # Spring Boot + Gradle
│   ├── build.gradle(.kts)
│   └── src/
│       ├── main/java/ar/turnos/
│       │   ├── TurnosApplication.java
│       │   ├── shared/              # tenancy (filtro, contexto), errores, seguridad, util de tiempo
│       │   ├── auth/                # registro, login, verificación, recuperación
│       │   ├── consultorio/         # consultorio, horarios, prestaciones, bloqueos, sillón
│       │   ├── agenda/              # cálculo de disponibilidad
│       │   ├── turnos/              # turnos, tokens de gestión, reprogramación
│       │   ├── listaespera/         # inscripciones, ofertas, aceptación
│       │   ├── pacientes/           # pacientes
│       │   ├── clinica/             # odontograma, evoluciones
│       │   ├── notificaciones/      # outbox, plantillas, envío, tarea programada
│       │   ├── auditoria/           # registro y consulta
│       │   ├── reportes/            # ausentismo, exportación
│       │   └── admin/               # administración SaaS
│       │   # Cada módulo: api/ (controllers, DTO) · application/ (servicios)
│       │   #              domain/ (entidades, enums, eventos) · infrastructure/ (repositorios)
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── db/migration/        # Flyway: V1__esquema.sql, ...
│       │   └── templates/email/     # plantillas de email (Thymeleaf o similar)
│       └── test/java/ar/turnos/     # unitarios + integración con Testcontainers
├── frontend/                        # TypeScript sin framework + Vite
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   ├── public/_redirects            # (Netlify) o equivalente: /api/* → backend; /* → index.html
│   └── src/
│       ├── main.ts                  # arranque y router
│       ├── router.ts                # router con History API
│       ├── shared/
│       │   ├── api.ts               # cliente fetch: credentials, CSRF, ProblemDetail
│       │   ├── dom.ts               # helpers de creación de elementos
│       │   ├── fechas.ts            # formateo en la zona del consultorio
│       │   └── components/          # componentes reutilizables (Web Components nativos o funciones)
│       ├── pages/
│       │   ├── publico/             # reserva, gestión del turno, oferta, lista de espera
│       │   ├── auth/                # registro, login, verificación
│       │   ├── panel/               # agenda, pacientes, ficha, configuración, reportes
│       │   └── admin/
│       └── styles/                  # CSS con tokens del design-system/
├── design-system/                   # (fase visual-design de la fundación)
├── knowledge-base/
└── openspec/
```

**Reglas de dependencia entre módulos del backend**:

- Un módulo usa a otro solo a través de sus servicios de `application/`, nunca a través de sus repositorios.
- `listaespera` reacciona a eventos de `turnos`; `turnos` no conoce a `listaespera`.
- `shared` no depende de ningún módulo.

## Multi-tenancy (implementación)

1. Las entidades de negocio heredan de una clase base con `consultorio_id` y la anotación `@FilterDef`/`@Filter("tenant")`.
2. Un filtro web o un interceptor resuelve el consultorio de la sesión y habilita el filtro de Hibernate con ese id en cada request autenticada.
3. Las rutas públicas resuelven el consultorio por slug o por token (RN-MT-02) y habilitan el filtro con ese valor.
4. Los inserts toman el `consultorio_id` del contexto, nunca del cuerpo de la request.
5. **Test obligatorio**: suite de integración que crea dos consultorios y verifica que ningún endpoint devuelva ni modifique datos del otro.

## Seguridad

- **Autenticación**:
  - Spring Security con sesión en el servidor y cookie `SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`).
  - Login por formulario JSON.
  - Contraseñas con BCrypt.
  - Bloqueo por intentos fallidos (RN-AU-05).
  - **Suposición SU-06**: sesiones en la base (Spring Session JDBC), para que sobrevivan a los reinicios del hosting gratuito.
- **CSRF**: `CookieCsrfTokenRepository`. El cliente fetch del frontend envía el header `X-XSRF-TOKEN`.
- **Autorización**: `@PreAuthorize` por rol en los controllers, además del filtro de tenant en la persistencia.
- **Pacientes sin cuenta**: tokens opacos de 128 bits o más, guardados como hash SHA-256, con vencimiento y revocación (RN-SG-01).
- **Validación de input**: Bean Validation en los DTO, normalización de DNI y email, y límites de longitud. Ningún HTML se acepta ni se renderiza sin escapar (el frontend usa `textContent`, nunca `innerHTML` con datos).
- **Límite de tasa**: en memoria (por ejemplo, Bucket4j) sobre `/api/public/**` y `/api/auth/**` (RN-SG-05).
- **Headers**: CSP estricta en el hosting estático, HSTS, `X-Content-Type-Options` y `Referrer-Policy: no-referrer`, para que los tokens de la URL no se filtren a terceros.
- **Secretos**: solo en variables de entorno del hosting. Nunca en el repositorio. `.env` en `.gitignore`.
- **Datos personales**: datos mínimos, cifrado en tránsito (TLS) y exportación disponible. Las obligaciones legales se detallan en `09_decisiones_y_supuestos.md` y en PA-07.

## Calidad y tests (prioridad: velocidad)

Mínimo innegociable, aun priorizando velocidad (DD-07):

| Área | Tipo de test |
|---|---|
| Solapamiento y sobreturno | Integración con PostgreSQL real (Testcontainers), porque la restricción de exclusión no existe en H2 |
| Regla de 24 h y transiciones de estado | Unitarios |
| Aceptación concurrente de ofertas | Integración con dos hilos simultáneos |
| Aislamiento entre consultorios | Integración (ver arriba) |
| Cálculo de disponibilidad | Unitarios con casos de borde: franjas, bloqueos, cambio de día, zona horaria |

En el frontend, sin tests automáticos obligatorios en v1: solo una verificación manual del flujo de reserva en el celular.

## Variables de entorno

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://host:5432/turnos` | Sí |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `turnos_app` | Sí |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | — | Sí |
| `APP_BASE_URL` | URL pública del frontend, para armar los links de los emails | `https://turnos.example.app` | No |
| `APP_MAIL_FROM` | Remitente de los emails | `Turnos <no-reply@…>` | No |
| `SPRING_MAIL_HOST` / `PORT` / `USERNAME` / `PASSWORD` | SMTP del proveedor de email (o `APP_MAIL_API_KEY` si se usa API) | — | Sí (credenciales) |
| `APP_MAIL_DAILY_QUOTA` | Cuota diaria de envíos del plan gratuito | `300` | No |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | Administrador SaaS inicial (seed) | — | Sí |
| `APP_INTERNAL_TICK_SECRET` | Secreto del endpoint de *keep-alive* y tick externo | — | Sí |
| `APP_TIMEZONE_DEFAULT` | Zona horaria por defecto | `America/Argentina/Buenos_Aires` | No |
| `SPRING_PROFILES_ACTIVE` | Perfil | `dev` / `prod` | No |
