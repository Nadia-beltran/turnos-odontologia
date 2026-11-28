package ar.turnos.shared.tenancy;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ar.turnos.shared.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Base de la suite de aislamiento entre consultorios (design D6).
 *
 * <p>Cómo extenderla en cada ficha: crear {@code XxxAislamientoIntegrationTest extends AbstractTenantIsolationTest},
 * insertar datos de la entidad en {@link #consultorioA()} y {@link #consultorioB()} con {@link #comoConsultorio},
 * y repetir los casos de listado propio, búsqueda por id ajeno, alta, modificación ajena y consulta sin contexto.
 * Los consultorios son ficticios y llevan un sufijo aleatorio, así que no chocan en el contexto compartido.
 */
public abstract class AbstractTenantIsolationTest extends AbstractIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TransactionTemplate tx;

    @PersistenceContext
    protected EntityManager em;

    private UUID consultorioA;
    private UUID consultorioB;

    @BeforeEach
    void crearConsultorios() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        consultorioA = crearConsultorio("consultorio-prueba-a-" + sufijo);
        consultorioB = crearConsultorio("consultorio-prueba-b-" + sufijo);
    }

    protected UUID consultorioA() {
        return consultorioA;
    }

    protected UUID consultorioB() {
        return consultorioB;
    }

    /** Ejecuta el bloque en una transacción, con el consultorio dado en el contexto. */
    protected <T> T comoConsultorio(UUID consultorioId, Supplier<T> bloque) {
        return TenantContext.ejecutarCon(consultorioId, () -> tx.execute(status -> bloque.get()));
    }

    private UUID crearConsultorio(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into consultorio (id, nombre, slug, email_contacto, estado) values (?, ?, ?, ?, 'ACTIVO')",
                id, "Consultorio de prueba", slug, "contacto@ejemplo.test");
        return id;
    }
}
