package tcc.gamers.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import tcc.gamers.TCCPlugin;

/**
 * Skript effect that updates a registered {@link SkriptMutable} value at runtime.
 *
 * <p>Supported syntax patterns:</p>
 * <ul>
 *   <li>{@code update tcc %string% to %object%}</li>
 *   <li>{@code update tcc %string% with %string% and %number%}</li>
 *   <li>{@code update tcc %string% using %objects%}</li>
 * </ul>
 *
 * <p>In every pattern, expression {@code 0} is the string identifier and expression
 * {@code 1} is the value — matching the fixed index convention in {@link SkriptMutable}.</p>
 *
 * <p>If the identifier is not registered, the effect silently does nothing.
 * If the update fails (wrong type, null value, etc.), a Skript error is emitted
 * and the previous value is preserved.</p>
 */
public class EffectDynamicUpdate extends Effect {

    private final TCCPlugin plugin;

    /**
     * Registers this effect with the given {@link SkriptAddon}.
     * Call this once during plugin enable.
     *
     * @param addon the addon to register the syntax with
     */
    public static void register(@NotNull SkriptAddon addon) {
        SyntaxInfo<EffectDynamicUpdate> info = SyntaxInfo.builder(EffectDynamicUpdate.class)
                .addPatterns(
                        "update tcc %string% to %number%",
                        "update tcc %string% with %string% and %number%",
                        "update tcc %string% using %objects%"
                )
                .build();


        addon.syntaxRegistry().register(SyntaxRegistry.EFFECT, info);
    }

    private Expression<?>[] expressions;
    private int matchedPattern;

    public EffectDynamicUpdate() {
        this.plugin = TCCPlugin.getInstance();
    }

    @Override
    public boolean init(
            Expression<?>[] exprs,
            int matchedPattern,
            Kleenean isDelayed,
            ParseResult parseResult
    ) {
        this.expressions    = exprs;
        this.matchedPattern = matchedPattern;
        return true;
    }

    @Override
    protected void execute(@NotNull Event event) {
        @SuppressWarnings("unchecked")
        String id = ((Expression<String>) this.expressions[0]).getSingle(event);

        if (id == null) return;

        SkriptMutable<?> mutable = plugin.getSkriptMutableRegistry().getMutable(id);

        if (mutable == null) return;

        ExpressionBundle bundle = ExpressionBundle.builder()
                .event(event)
                .expressions(this.expressions)
                .pattern(this.matchedPattern)
                .build();

        try {
            mutable.save(bundle);
        } catch (Exception e) {
            Skript.error("Failed to update TCC mutable '" + id + "': " + e.getMessage());
        }
    }

    @Override
    public @NotNull String toString(Event event, boolean debug) {
        return "update tcc mutable '" + expressions[0].toString(event, debug) + "'";
    }
}