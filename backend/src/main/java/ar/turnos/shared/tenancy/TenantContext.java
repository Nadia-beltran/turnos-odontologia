package ar.turnos.shared.tenancy;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Consultorio actual del hilo (design D2). Solo se establece con un bloque que lo restaura al salir;
 * no hay {@code set} suelto, para que no se filtre a la siguiente request del mismo hilo del pool.
 */
public final class TenantContext {

    /** Estado del hilo: consultorio (o null) y si está en modo sistema. */
    private record Estado(UUID consultorioId, boolean sistema) {
    }

    private static final ThreadLocal<Estado> ESTADO = new ThreadLocal<>();

    private TenantContext() {
    }

    public static <T> T ejecutarCon(UUID consultorioId, Supplier<T> bloque) {
        if (consultorioId == null) {
            throw new IllegalArgumentException("consultorioId es obligatorio");
        }
        return con(new Estado(consultorioId, false), bloque);
    }

    public static void ejecutarCon(UUID consultorioId, Runnable bloque) {
        ejecutarCon(consultorioId, () -> {
            bloque.run();
            return null;
        });
    }

    /** Lectura sin consultorio, solo dentro del bloque. Usos permitidos: ver package-info. */
    public static <T> T comoSistema(Supplier<T> bloque) {
        return con(new Estado(null, true), bloque);
    }

    public static void comoSistema(Runnable bloque) {
        comoSistema(() -> {
            bloque.run();
            return null;
        });
    }

    public static Optional<UUID> actual() {
        Estado e = ESTADO.get();
        return e == null ? Optional.empty() : Optional.ofNullable(e.consultorioId());
    }

    public static UUID requerido() {
        return actual().orElseThrow(TenantContextAusenteException::new);
    }

    public static boolean esModoSistema() {
        Estado e = ESTADO.get();
        return e != null && e.sistema();
    }

    private static <T> T con(Estado nuevo, Supplier<T> bloque) {
        Estado anterior = ESTADO.get();
        ESTADO.set(nuevo);
        try {
            return bloque.get();
        } finally {
            if (anterior == null) {
                ESTADO.remove();
            } else {
                ESTADO.set(anterior);
            }
        }
    }
}
