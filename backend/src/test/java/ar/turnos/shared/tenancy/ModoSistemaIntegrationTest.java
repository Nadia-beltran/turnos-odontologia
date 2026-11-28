package ar.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec tenant-isolation: modo sistema explícito y acotado. */
class ModoSistemaIntegrationTest extends AbstractTenantIsolationTest {

    private String valorA;
    private String valorB;

    @BeforeEach
    void datos() {
        String s = UUID.randomUUID().toString();
        valorA = "valor-sistema-1-" + s;
        valorB = "valor-sistema-2-" + s;
        jdbc.update("insert into prueba_tenant (id, consultorio_id, valor) values (?, ?, ?)", UUID.randomUUID(), consultorioA(), valorA);
        jdbc.update("insert into prueba_tenant (id, consultorio_id, valor) values (?, ?, ?)", UUID.randomUUID(), consultorioB(), valorB);
    }

    private List<EntidadPruebaTenant> buscar(String valor) {
        return em.createQuery("select e from EntidadPruebaTenant e where e.valor = :v", EntidadPruebaTenant.class)
                .setParameter("v", valor).getResultList();
    }

    @Test
    void busquedaEnModoSistemaLoEncuentra() {
        List<EntidadPruebaTenant> r = TenantContext.comoSistema(() -> tx.execute(status -> buscar(valorA)));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).getConsultorioId()).isEqualTo(consultorioA());
    }

    @Test
    void mismoPedidoSinDeclararElModoFalla() {
        assertThatThrownBy(() -> tx.execute(status -> buscar(valorA))).isInstanceOf(TenantContextAusenteException.class);
    }

    @Test
    void elModoNoSobreviveAlBloque() {
        TenantContext.comoSistema(() -> tx.execute(status -> buscar(valorA)));
        assertThatThrownBy(() -> tx.execute(status -> buscar(valorA))).isInstanceOf(TenantContextAusenteException.class);
        assertThatThrownBy(() -> TenantContext.comoSistema(() -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tx.execute(status -> buscar(valorA))).isInstanceOf(TenantContextAusenteException.class);
    }

    @Test
    void consultorioDentroDelModoSistema() {
        TenantContext.comoSistema(() -> tx.execute(status -> {
            assertThat(TenantContext.ejecutarCon(consultorioA(), () -> buscar(valorB))).isEmpty();
            assertThat(TenantContext.ejecutarCon(consultorioA(), () -> buscar(valorA))).hasSize(1);
            assertThat(TenantContext.esModoSistema()).isTrue();
            assertThat(buscar(valorB)).hasSize(1);
            return null;
        }));
    }
}
