package ar.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void restauraTrasEjecucionNormal() {
        TenantContext.ejecutarCon(a, () -> assertThat(TenantContext.requerido()).isEqualTo(a));
        assertThat(TenantContext.actual()).isEmpty();
    }

    @Test
    void restauraTrasExcepcion() {
        assertThatThrownBy(() -> TenantContext.ejecutarCon(a, () -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.actual()).isEmpty();
        assertThat(TenantContext.esModoSistema()).isFalse();
    }

    @Test
    void anidadoRestauraElAnterior() {
        TenantContext.ejecutarCon(a, () -> {
            TenantContext.ejecutarCon(b, () -> assertThat(TenantContext.requerido()).isEqualTo(b));
            assertThat(TenantContext.requerido()).isEqualTo(a);
        });
    }

    @Test
    void consultorioDentroDeModoSistemaYAlReves() {
        TenantContext.comoSistema(() -> {
            TenantContext.ejecutarCon(a, () -> {
                assertThat(TenantContext.requerido()).isEqualTo(a);
                assertThat(TenantContext.esModoSistema()).isFalse();
            });
            assertThat(TenantContext.esModoSistema()).isTrue();
        });
        TenantContext.ejecutarCon(a, () -> {
            TenantContext.comoSistema(() -> assertThat(TenantContext.esModoSistema()).isTrue());
            assertThat(TenantContext.requerido()).isEqualTo(a);
            assertThat(TenantContext.esModoSistema()).isFalse();
        });
    }

    @Test
    void requeridoLanzaSinContextoYEnModoSistema() {
        assertThatThrownBy(TenantContext::requerido).isInstanceOf(TenantContextAusenteException.class);
        TenantContext.comoSistema(() -> assertThatThrownBy(TenantContext::requerido)
                .isInstanceOf(TenantContextAusenteException.class));
    }

    @Test
    void modoSistemaNoSobreviveAlBloque() {
        TenantContext.comoSistema(() -> { });
        assertThat(TenantContext.esModoSistema()).isFalse();
        assertThatThrownBy(() -> TenantContext.comoSistema(() -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.esModoSistema()).isFalse();
    }

    @Test
    void elEstadoNoSeVeDesdeOtroHilo() {
        AtomicReference<Boolean> vacio = new AtomicReference<>();
        TenantContext.ejecutarCon(a, () -> {
            Thread t = new Thread(() -> vacio.set(TenantContext.actual().isEmpty()));
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(vacio.get()).isTrue();
    }
}
