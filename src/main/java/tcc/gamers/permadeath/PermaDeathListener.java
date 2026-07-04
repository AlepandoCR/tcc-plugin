package tcc.gamers.permadeath;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PermaDeathListener implements Listener {

    private final @NotNull PermaDeathManager permaDeathManager;

    public PermaDeathListener(@NotNull PermaDeathManager permaDeathManager) {
        this.permaDeathManager = permaDeathManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        permaDeathManager.addPlayer(player);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        permaDeathManager.onPlayerDeath(player);
    }
}