package tcc.gamers.item.particle;

import net.democracycraft.democracyLib.api.dialog.factory.DialogFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ui.particle.ParticleCustomizationMenu;

public class ParticleProjectorListener implements Listener {

    private final ParticleProjectorManager manager;
    private final TCCPlugin plugin;

    public ParticleProjectorListener(@NotNull TCCPlugin plugin, @NotNull ParticleProjectorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onSlotChange(@NotNull PlayerItemHeldEvent event) {
        scheduleCheck(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleCheck(player);
        }
    }

    @EventHandler
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        scheduleCheck(event.getPlayer());
    }

    @EventHandler
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleCheck(player);
        }
    }

    @EventHandler
    public void onSwapHand(@NotNull PlayerSwapHandItemsEvent event) {
        scheduleCheck(event.getPlayer());
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        scheduleCheck(event.getPlayer());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onProjectorClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();

        if(item == null) return;
        if (ParticleProjectorHelper.isParticleProjector(item)) {

            event.setCancelled(true);
            Player player = event.getPlayer();
            player.showDialog(DialogFactory.create(new ParticleCustomizationMenu(plugin.getConfigManager().getParticleProjectorConfig(), plugin, item)));
        }
    }

    private void scheduleCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isValid()) {
                manager.checkPlayerEquipped(player);
            }
        }, 1L);
    }
}