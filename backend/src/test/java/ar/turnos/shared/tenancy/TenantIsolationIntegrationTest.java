package ar.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Spec tenant-isolation: lecturas, altas y modificaciones limitadas al consultorio del contexto. */
class TenantIsolationIntegrationTest extends AbstractTenantIsolationTest {

    private UUID insertar(UUID consultorio, String valor) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into prueba_tenant (id, consultorio_id, valor) values (?, ?, ?)", id, consultorio, valor);
        return id;
    }

    private String valorEnBase(UUID id) {
        return jdbc.queryForObject("select valor from prueba_tenant where id = ?", String.class, id);
    }

    private List<EntidadPruebaTenant> listar() {
        return em.createQuery("select e from EntidadPruebaTenant e", EntidadPruebaTenant.class).getResultList();
    }

    @Test
    void listadoPropio() {
        String s = UUID.randomUUID().toString();
        insertar(consultorioA(), "a-" + s);
        insertar(consultorioB(), "b-" + s);
        List<EntidadPruebaTenant> vistos = comoConsultorio(consultorioA(), this::listar);
        assertThat(vistos).isNotEmpty().allMatch(e -> e.getConsultorioId().equals(consultorioA()));
        assertThat(vistos).extracting(EntidadPruebaTenant::getValor).contains("a-" + s).doesNotContain("b-" + s);
    }

    @Test
    void busquedaPorIdAjenoNoLoEncuentra() {
        UUID idB = insertar(consultorioB(), "b-" + UUID.randomUUID());
        assertThat(comoConsultorio(consultorioA(), () -> em.find(EntidadPruebaTenant.class, idB))).isNull();
        assertThat(comoConsultorio(consultorioB(), () -> em.find(EntidadPruebaTenant.class, idB))).isNotNull();
    }

    @Test
    void consultaSinContextoFalla() {
        insertar(consultorioA(), "a-" + UUID.randomUUID());
        assertThatThrownBy(() -> tx.execute(status -> listar())).isInstanceOf(TenantContextAusenteException.class);
    }

    @Test
    void altaEnElContextoDeA() {
        String valor = "alta-" + UUID.randomUUID();
        comoConsultorio(consultorioA(), () -> {
            em.persist(new EntidadPruebaTenant(valor));
            return null;
        });
        UUID guardado = jdbc.queryForObject("select consultorio_id from prueba_tenant where valor = ?", UUID.class, valor);
        assertThat(guardado).isEqualTo(consultorioA());
    }

    @Test
    void altaConConsultorioAjenoFallaYNoGuardaNada() throws Exception {
        String valor = "ajeno-" + UUID.randomUUID();
        EntidadPruebaTenant e = new EntidadPruebaTenant(valor);
        Field campo = BaseTenantEntity.class.getDeclaredField("consultorioId");
        campo.setAccessible(true);
        campo.set(e, consultorioB());
        assertThatThrownBy(() -> comoConsultorio(consultorioA(), () -> {
            em.persist(e);
            em.flush();
            return null;
        })).isInstanceOf(TenantViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from prueba_tenant where valor = ?", Integer.class, valor)).isZero();
    }

    @Test
    void modificacionAjenaFallaYLaFilaQuedaIgual() {
        UUID id = insertar(consultorioB(), "orig-" + UUID.randomUUID());
        String original = valorEnBase(id);
        assertThatThrownBy(() -> TenantContext.comoSistema(() -> tx.execute(status -> {
            EntidadPruebaTenant e = em.find(EntidadPruebaTenant.class, id);
            TenantContext.ejecutarCon(consultorioA(), () -> {
                e.setValor("modificado");
                em.flush();
            });
            return null;
        }))).isInstanceOf(TenantViolationException.class);
        assertThat(valorEnBase(id)).isEqualTo(original);
    }

    @Test
    void borradoAjenoFallaYLaFilaSigue() {
        UUID id = insertar(consultorioB(), "borrar-" + UUID.randomUUID());
        assertThatThrownBy(() -> TenantContext.comoSistema(() -> tx.execute(status -> {
            EntidadPruebaTenant e = em.find(EntidadPruebaTenant.class, id);
            TenantContext.ejecutarCon(consultorioA(), () -> {
                em.remove(e);
                em.flush();
            });
            return null;
        }))).isInstanceOf(TenantViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from prueba_tenant where id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void contextoRestauradoConOSinError() {
        comoConsultorio(consultorioA(), () -> 1);
        assertThat(TenantContext.actual()).isEmpty();
        assertThatThrownBy(() -> comoConsultorio(consultorioA(), () -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.actual()).isEmpty();
        assertThatThrownBy(() -> tx.execute(status -> listar())).isInstanceOf(TenantContextAusenteException.class);
    }
}
