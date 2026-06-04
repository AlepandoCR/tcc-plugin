package tcc.gamers.skript;

import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bundles the Skript {@link Expression} array, the current {@link Event}, and the
 * matched pattern index into a single, immutable snapshot.
 *
 * <p>Instances are created by {@link EffectDynamicUpdate} at execution time and
 * handed to {@link SkriptMutable#save(ExpressionBundle)}. The bundle itself does
 * not interpret the expressions — it only provides safe, bounds-checked accessors.</p>
 *
 * <p>Index convention:</p>
 * <ul>
 *   <li>Index {@code 0} — always the string identifier (handled by the effect).</li>
 *   <li>Index {@code 1} — always the value payload (read by {@link SkriptMutable}).</li>
 * </ul>
 */
public class ExpressionBundle {

    private final Event event;
    private final Expression<?>[] expressions;
    private final int matchedPattern;

    private ExpressionBundle(@NotNull Builder builder) {
        this.event          = builder.event;
        this.expressions    = builder.expressions;
        this.matchedPattern = builder.matchedPattern;
    }

    /** Returns a new {@link Builder}. */
    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Evaluates the expression at {@code index} and returns a single value.
     *
     * @param index expression array position
     * @param <T>   expected return type (unchecked — caller is responsible)
     * @return the resolved value, or {@code null} if out of bounds or unresolvable
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getSingle(int index) {
        if (index < 0 || index >= expressions.length || expressions[index] == null) return null;
        return (T) expressions[index].getSingle(event);
    }

    /**
     * Evaluates the expression at {@code index} and returns all values as an array.
     *
     * @param index expression array position
     * @param <T>   expected element type (unchecked — caller is responsible)
     * @return the resolved array, or {@code null} if out of bounds or unresolvable
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T[] getArray(int index) {
        if (index < 0 || index >= expressions.length || expressions[index] == null) return null;
        return (T[]) expressions[index].getArray(event);
    }

    /** Returns the pattern index that was matched by the Skript parser. */
    public int getMatchedPattern() {
        return matchedPattern;
    }

    /** Returns the event that triggered the executing effect. */
    public @NotNull Event getEvent() {
        return event;
    }

    /** Fluent builder for {@link ExpressionBundle}. */
    public static class Builder {

        private Event event;
        private Expression<?>[] expressions = new Expression[0];
        private int matchedPattern = 0;

        /** Sets the event. Required — {@link #build()} throws if missing. */
        public Builder event(@NotNull Event event) {
            this.event = event;
            return this;
        }

        /** Sets the full expression array from the Skript effect. */
        public Builder expressions(@NotNull Expression<?>[] expressions) {
            this.expressions = expressions;
            return this;
        }

        /** Sets the matched pattern index. Defaults to {@code 0}. */
        public Builder pattern(int matchedPattern) {
            this.matchedPattern = matchedPattern;
            return this;
        }

        /**
         * Builds the bundle.
         *
         * @throws IllegalStateException if {@link #event} was not set
         */
        public ExpressionBundle build() {
            if (this.event == null) throw new IllegalStateException("Event must not be null");
            return new ExpressionBundle(this);
        }
    }
}