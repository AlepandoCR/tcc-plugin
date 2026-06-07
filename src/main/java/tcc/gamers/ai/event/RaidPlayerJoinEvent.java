package tcc.gamers.ai.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.raid.Raid;

public class RaidPlayerJoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final @NotNull Raid raid;
    private final @NotNull Player player;

    public RaidPlayerJoinEvent(@NotNull Raid raid, @NotNull Player player) {
        this.raid = raid;
        this.player = player;
    }

    public @NotNull Raid getRaid() {
        return this.raid;
    }

    public @NotNull Player getPlayer() {
        return this.player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}