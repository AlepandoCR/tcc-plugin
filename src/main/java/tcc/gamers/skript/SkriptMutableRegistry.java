package tcc.gamers.skript;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry that maps string identifiers to {@link SkriptMutable} instances.
 *
 * <p>Identifiers are stored and looked up in lower-case so Skript scripts are
 * case-insensitive when referencing mutable values.</p>
 *
 * <p>Register values once at plugin startup:</p>
 * <pre>{@code
 * registry.registerMutable("score",       new SkriptMutable<>(0,         Integer.class));
 * registry.registerMutable("player-name", new SkriptMutable<>("unknown", String.class));
 * }</pre>
 */
public class SkriptMutableRegistry {

    private final Map<String, SkriptMutable<?>> registry = new HashMap<>();

    /**
     * Registers a {@link SkriptMutable} under the given identifier.
     *
     * <p>Overwrites any previously registered value for the same identifier.</p>
     *
     * @param identifier case-insensitive key used in Skript syntax
     * @param mutable    the mutable instance to store
     */
    public void registerMutable(@NotNull String identifier, @NotNull SkriptMutable<?> mutable) {
        registry.put(identifier.toLowerCase(), mutable);
    }

    public void unregisterMutable(@NotNull String identifier) {
        registry.remove(identifier.toLowerCase());
    }

    /**
     * Returns the {@link SkriptMutable} registered under the given identifier,
     * or {@code null} if no value has been registered for it.
     *
     * @param identifier case-insensitive key
     * @return the registered instance, or {@code null}
     */
    @Nullable
    public SkriptMutable<?> getMutable(@NotNull String identifier) {
        return registry.get(identifier.toLowerCase());
    }
}