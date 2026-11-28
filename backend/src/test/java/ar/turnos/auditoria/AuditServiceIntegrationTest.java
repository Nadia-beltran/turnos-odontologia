package ar.turnos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ar.turnos.shared.tenancy.AbstractTenantIsolationTest;

/** Spec audit-log (solo datos ficticios). */
@ExtendWith(OutputCaptureExtension.class)
class AuditServiceIntegrationTest extends AbstractTenantIsolationTest {

    @Autowired
    AuditService auditoria;

    @AfterEach
    void limpiarRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String accionUnica() {
        return "PRUEBA_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private int filas(String accion) {
        return jdbc.queryForObject("select count(*) from auditoria where accion = ?", Integer.class, accion);
    }

    @Test
    void accionEnElContextoDeUnConsultorio() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String accion = accionUnica();
        UUID entidadId = UUID.randomUUID();

        comoConsultorio(consultorioA(), () ->
                auditoria.registrar(accion, "prueba", entidadId, Map.of("campo", "valor-ficticio")));

        Map<String, Object> fila = jdbc.queryForMap(
                "select consultorio_id, entidad, entidad_id, datos::text as datos, host(ip) as ip, actor_tipo, created_at"
                        + " from auditoria where accion = ?", accion);
        assertThat(fila.get("consultorio_id")).isEqualTo(consultorioA());
        assertThat(fila.get("entidad")).isEqualTo("prueba");
        assertThat(fila.get("entidad_id")).isEqualTo(entidadId);
        assertThat((String) fila.get("datos")).contains("\"campo\"").contains("valor-ficticio");
        assertThat(fila.get("ip")).isEqualTo("192.0.2.10");
        assertThat(fila.get("actor_tipo")).isEqualTo("SISTEMA");
        assertThat(fila.get("created_at")).isNotNull();
    }

    @Test
    void accionSinRequestNiConsultorio() {
        RequestContextHolder.resetRequestAttributes(); // SpringBootTest deja una request simulada en el hilo
        String accion = accionUnica();
        tx.execute(status -> auditoria.registrar(accion, null, null, null));

        Map<String, Object> fila = jdbc.queryForMap(
                "select consultorio_id, usuario_id, ip, actor_tipo from auditoria where accion = ?", accion);
        assertThat(fila.get("actor_tipo")).isEqualTo("SISTEMA");
        assertThat(fila.get("consultorio_id")).isNull();
        assertThat(fila.get("usuario_id")).isNull();
        assertThat(fila.get("ip")).isNull();
    }

    @Test
    void accionSinNombreOMuyLargaSeRechaza() {
        int antes = jdbc.queryForObject("select count(*) from auditoria", Integer.class);
        assertThatThrownBy(() -> tx.execute(status -> auditoria.registrar("  ", "prueba", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tx.execute(status -> auditoria.registrar("X".repeat(61), "prueba", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("select count(*) from auditoria", Integer.class)).isEqualTo(antes);
    }

    @Test
    void operacionRevertidaNoDejaFila() {
        String accion = accionUnica();
        tx.execute(status -> {
            auditoria.registrar(accion, "prueba", null, null);
            status.setRollbackOnly();
            return null;
        });
        assertThat(filas(accion)).isZero();
    }

    @Test
    void actorUsuarioSinUsuarioLoRechazaLaBase() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into auditoria (actor_tipo, accion) values ('USUARIO', ?)", accionUnica()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void noSePuedeModificarNiBorrar() {
        String accion = accionUnica();
        tx.execute(status -> auditoria.registrar(accion, "prueba", null, null));

        assertThatThrownBy(() -> jdbc.update("update auditoria set entidad = 'otra' where accion = ?", accion))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("delete from auditoria where accion = ?", accion))
                .isInstanceOf(DataAccessException.class);
        assertThat(filas(accion)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select entidad from auditoria where accion = ?", String.class, accion))
                .isEqualTo("prueba");
    }

    @Test
    void elLogNoContieneElDetalle(CapturedOutput output) {
        String accion = accionUnica();
        tx.execute(status -> auditoria.registrar(accion, "prueba", UUID.randomUUID(),
                Map.of("paciente", "Paciente Ficticio DNI 00000000")));

        assertThat(output.getAll()).contains(accion).doesNotContain("Paciente Ficticio").doesNotContain("00000000");
    }
}
