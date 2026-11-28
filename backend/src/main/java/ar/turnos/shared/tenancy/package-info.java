/**
 * Multi-tenancy de esquema compartido (RN-MT-01, RN-MT-02, DD-01).
 *
 * <p><b>Cómo extenderla.</b> Una entidad de negocio extiende {@link ar.turnos.shared.tenancy.BaseTenantEntity}
 * (tabla con {@code consultorio_id uuid not null}, indexado). El filtro {@code tenant} queda habilitado en
 * toda sesión y lee {@link ar.turnos.shared.tenancy.TenantContext}; sin consultorio falla cerrado. El
 * consultorio nunca viene del cliente: se establece con {@code TenantContext.ejecutarCon(id, ...)}.
 * Si la columna es nullable (p. ej. {@code usuario}), la entidad declara {@code @Filter(name = "tenant")}
 * directamente (el {@code @FilterDef} es global por nombre). Cada ficha repite la suite de aislamiento
 * extendiendo {@code AbstractTenantIsolationTest}.
 *
 * <p><b>Modo sistema.</b> {@code TenantContext.comoSistema(...)} deja leer sin consultorio solo dentro del
 * bloque. Usarlo únicamente cuando el consultorio todavía no se conoce (login por email, token por hash,
 * procesos que recorren consultorios) y pasar a {@code ejecutarCon} apenas se conozca. Cada uso se
 * revisa en el PR.
 *
 * <p><b>Consultas nativas.</b> El filtro de Hibernate NO se aplica a SQL nativo: toda consulta nativa sobre
 * tablas de negocio debe incluir {@code consultorio_id = :consultorioId} con {@code TenantContext.requerido()}.
 */
package ar.turnos.shared.tenancy;
