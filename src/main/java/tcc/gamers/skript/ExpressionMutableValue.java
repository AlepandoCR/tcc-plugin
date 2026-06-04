package tcc.gamers.skript;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import tcc.gamers.TCCPlugin;

/**
 * Skript expression that reads a registered {@link SkriptMutable} value at runtime.
 *
 * <p>Syntax:</p>
 * <pre>{@code tcc value of %string%}</pre>
 *
 * <p>Example usage in Skript:</p>
 * <pre>{@code
 * set {_karma} to tcc value of "cloudsKarma"
 * send "Clouds karma: %tcc value of ""cloudsKarma""%"
 * }</pre>
 */
public class ExpressionMutableValue extends SimpleExpression<Object> {

    private final TCCPlugin plugin;
    private Expression<String> idExpression;

    public static void register(@NotNull SkriptAddon addon) {
        SyntaxInfo.Expression<ExpressionMutableValue, Object> info =
                SyntaxInfo.Expression.builder(ExpressionMutableValue.class, Object.class)
                        .addPattern("tcc value of %string%")
                        .build();

        addon.syntaxRegistry().register(SyntaxRegistry.EXPRESSION, info);
    }
    public ExpressionMutableValue() {
        this.plugin = TCCPlugin.getInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            @NotNull Expression<?>[] exprs,
            int matchedPattern,
            @NotNull Kleenean isDelayed,
            @NotNull ParseResult parseResult
    ) {
        this.idExpression = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected Object[] get(@NotNull Event event) {
        String id = idExpression.getSingle(event);
        if (id == null) return null;

        SkriptMutable<?> mutable = plugin.getSkriptMutableRegistry().getMutable(id);
        if (mutable == null) return null;

        return new Object[]{ mutable.get() };
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public @NotNull Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "tcc value of " + idExpression.toString(event, debug);
    }
}