package tcc.gamers.event.skript;

import ch.njol.skript.lang.util.SimpleEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

import java.util.function.Function;

public abstract class BaseSkriptEvent<T extends Event> {

    private final SkriptAddon addon;
    private final String name;
    private final Class<T> bukkitEventClass;
    private final String[] patterns;

    public BaseSkriptEvent(@NotNull SkriptAddon addon, @NotNull String name, @NotNull Class<T> bukkitEventClass, @NotNull String... patterns) {
        this.addon = addon;
        this.name = name;
        this.bukkitEventClass = bukkitEventClass;
        this.patterns = patterns;
    }

    public void register() {
        SyntaxRegistry syntaxRegistry = addon.registry(SyntaxRegistry.class);

        var eventInfo = BukkitSyntaxInfos.Event.builder(SimpleEvent.class, name)
                .addDescription("Evento tcc para " + name)
                .addEvent(bukkitEventClass)
                .addPatterns(patterns)
                .build();


        syntaxRegistry.register(
                BukkitSyntaxInfos.Event.KEY,
                eventInfo
        );

        registerEventValues();
    }

    protected abstract void registerEventValues();

    protected <V> void registerValue(@NotNull Class<V> valueClass, @NotNull Function<T, V> function) {
        EventValueRegistry registry = addon.registry(EventValueRegistry.class);

        registry.register(EventValue.simple(
                bukkitEventClass,
                valueClass,
                function::apply
        ));
    }
}