package ar.turnos.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/** Spec schema-migrations: migraciones al arrancar, btree_gist y esquema validado. */
class SchemaMigrationsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Environment env;

    @Test
    void alArrancarSeAplicaYRegistraLaMigracionV1() {
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void laExtensionBtreeGistQuedaInstalada() {
        Integer extension = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Integer.class);
        assertThat(extension).isEqualTo(1);
    }

    @Test
    void hibernateValidaElEsquemaYNuncaLoGenera() {
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
