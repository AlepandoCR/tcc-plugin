package tcc.gamers.skript;

import ch.njol.skript.Skript;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single mutable value that can be updated from a Skript effect.
 *
 * <p>Each instance wraps one raw value of type {@link ObjectType} and knows how
 * to update itself from an {@link ExpressionBundle}. The value always lives at
 * expression index {@code 1} - index {@code 0} is always the string identifier
 * used to look up this instance in the {@link SkriptMutableRegistry}.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * SkriptMutable<String>  playerName = new SkriptMutable<>("default", String.class);
 * SkriptMutable<Integer> score      = new SkriptMutable<>(0,         Integer.class);
 *
 * registry.registerMutable("player-name", playerName);
 * registry.registerMutable("score",       score);
 * }</pre>
 *
 * @param <ObjectType> the raw value type, must be {@link Serializable}
 */
public class SkriptMutable<ObjectType extends Serializable> {

    /**
     * Fixed expression index for the value payload.
     * Index 0 is always the string identifier; index 1 is always the value.
     */
    private static final int VALUE_INDEX = 1;

    private final @NotNull AtomicReference<ObjectType> value;
    private final @NotNull Class<ObjectType> type;
    private final @NotNull OnMutableSave<ObjectType> onMutableSave;

    /**
     * Creates a new {@code SkriptMutable} with an initial value.
     *
     * @param initialValue the starting value, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public SkriptMutable(@NotNull ObjectType initialValue, @NotNull OnMutableSave<ObjectType> onMutableSave) {
        this.onMutableSave = onMutableSave;
        this.value = new AtomicReference<>(initialValue);
        this.type = (Class<ObjectType>) initialValue.getClass(); // the type is the same as the classes generic
    }

    public void set(@NotNull ObjectType value) {
        this.value.set(value);
    }

    /**
     * Updates the stored value from the given bundle.
     *
     * <p>The new value is always read from expression index {@value VALUE_INDEX}.
     * If the extracted value is {@code null} or is not assignable to {@link ObjectType},
     * the update is rejected and a Skript error is emitted - the previous value is kept.</p>
     *
     * @param bundle the expression bundle provided by the executing effect
     */
    public void save(@NotNull ExpressionBundle bundle) {
        Object raw = bundle.getSingle(VALUE_INDEX);

        if (raw == null) {
            Skript.error("SkriptMutable '" + type.getSimpleName() + "': value at index "
                    + VALUE_INDEX + " resolved to null — update aborted.");
            return;
        }

        if (raw instanceof Number number && Number.class.isAssignableFrom(type)) {
            raw = coerceNumber(number);
        }

        if (!type.isInstance(raw)) {
            Skript.error("SkriptMutable '" + type.getSimpleName() + "': expected "
                    + type.getSimpleName() + " but got " + raw.getClass().getSimpleName()
                    + " — update aborted.");
            return;
        }

        ObjectType coerced = type.cast(raw);
        this.value.set(coerced);
        this.onMutableSave.onSave(coerced);
    }

    /**
     * Coerces a {@link Number} to the registered {@link ObjectType}.
     * Falls back to the original value if the type is unrecognized.
     */
    private Number coerceNumber(@NotNull Number number) {
        if (type == Integer.class) return number.intValue();
        if (type == Long.class)    return number.longValue();
        if (type == Double.class)  return number.doubleValue();
        if (type == Float.class)   return number.floatValue();
        if (type == Short.class)   return number.shortValue();
        if (type == Byte.class)    return number.byteValue();
        return number; // unknown Number subtype
    }

    /**
     * Returns the current value.
     *
     * @return the stored value, never {@code null}
     */
    public @NotNull ObjectType get() {
        return this.value.get();
    }

    /**
     * Returns the runtime class of the stored value type.
     *
     * @return the type class provided at construction time
     */
    public @NotNull Class<ObjectType> getType() {
        return this.type;
    }
}