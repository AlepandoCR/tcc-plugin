package tcc.gamers.ball.dash;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.item.ball.SoccerCleatsHelper;

public class DashInteractionListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    public DashInteractionListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDash(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();

        if(SoccerCleatsHelper.playerHasCleats(player)){
            plugin.getDashManager().tryDash(player);
        }
    }
}