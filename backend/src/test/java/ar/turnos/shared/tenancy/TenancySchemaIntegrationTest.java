package ar.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.turnos.shared.AbstractIntegrationTest;

/** Spec tenant-isolation: esquema de consultorio, usuario y sillón (solo datos ficticios). */
class TenancySchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private UUID consultorio(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into consultorio (id, nombre, slug, email_contacto, estado) values (?, ?, ?, ?, 'ACTIVO')",
                id, "Consultorio de prueba", slug, "contacto@ejemplo.test");
        return id;
    }

    private void usuario(String rol, UUID consultorioId) {
        jdbc.update("insert into usuario (id, consultorio_id, email, password_hash, nombre, rol) values (?, ?, ?, 'hash-ficticio', 'Usuario Prueba', ?)",
                UUID.randomUUID(), consultorioId, "u-" + UUID.randomUUID() + "@ejemplo.test", rol);
    }

    @Test
    void v2QuedaRegistradaYLasTablasExisten() {
        Integer v2 = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '2' and success", Integer.class);
        Integer tablas = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name in ('consultorio','usuario','sillon')",
                Integer.class);
        assertThat(v2).isEqualTo(1);
        assertThat(tablas).isEqualTo(3);
    }

    @Test
    void slugRepetidoEsRechazado() {
        String slug = "consultorio-prueba-" + UUID.randomUUID();
        consultorio(slug);
        assertThatThrownBy(() -> consultorio(slug)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void odontologoConConsultorioSeAcepta() {
        UUID c = consultorio("c-" + UUID.randomUUID());
        usuario("ODONTOLOGO", c);
    }

    @Test
    void administradorConConsultorioEsRechazado() {
        UUID c = consultorio("c-" + UUID.randomUUID());
        assertThatThrownBy(() -> usuario("ADMIN_SAAS", c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void recepcionSinConsultorioEsRechazada() {
        assertThatThrownBy(() -> usuario("RECEPCION", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void segundoSillonEsRechazado() {
        UUID c = consultorio("c-" + UUID.randomUUID());
        jdbc.update("insert into sillon (id, consultorio_id) values (?, ?)", UUID.randomUUID(), c);
        assertThatThrownBy(() -> jdbc.update("insert into sillon (id, consultorio_id) values (?, ?)", UUID.randomUUID(), c))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
