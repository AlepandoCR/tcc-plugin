package tcc.gamers.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;

import java.util.Collection;

public abstract class AbstractActionBarTask extends BukkitRunnable {

    protected final TCCPlugin plugin;

    public AbstractActionBarTask(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the task logic on each scheduled tick.
     */
    @Override
    public abstract void run();

    /**
     * Sends an action bar message to a single player.
     */
    protected void sendActionBar(@NotNull Player player, @NotNull Component component) {
        player.sendActionBar(component);
    }

    /**
     * Sends an action bar message to a collection of players.
     */
    protected void sendActionBar(@NotNull Collection<Player> players, @NotNull Component component) {
        players.forEach(player -> sendActionBar(player, component));
    }
}