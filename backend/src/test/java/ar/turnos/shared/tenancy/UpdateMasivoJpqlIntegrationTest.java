package ar.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Design 3.7: el filtro de Hibernate 7.4 SÍ se aplica a un UPDATE masivo en JPQL (resultado registrado en design.md). */
class UpdateMasivoJpqlIntegrationTest extends AbstractTenantIsolationTest {

    @Test
    void elUpdateMasivoEnJpqlRespetaElFiltro() {
        String s = UUID.randomUUID().toString();
        jdbc.update("insert into prueba_tenant (id, consultorio_id, valor) values (?, ?, ?)", UUID.randomUUID(), consultorioA(), "masivo-a-" + s);
        jdbc.update("insert into prueba_tenant (id, consultorio_id, valor) values (?, ?, ?)", UUID.randomUUID(), consultorioB(), "masivo-b-" + s);

        int afectadas = comoConsultorio(consultorioA(), () ->
                em.createQuery("update EntidadPruebaTenant e set e.valor = concat(e.valor, 'x') where e.valor like :p")
                        .setParameter("p", "masivo-%-" + s).executeUpdate());

        Integer bModificadas = jdbc.queryForObject(
                "select count(*) from prueba_tenant where valor = ?", Integer.class, "masivo-b-" + s + "x");
        assertThat(afectadas).isEqualTo(1);
        assertThat(bModificadas).isZero();
    }
}
