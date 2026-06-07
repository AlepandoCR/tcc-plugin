package tcc.gamers.ai.item.dragon;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.nms.nautilus.CustomNautilus;

public class DragonHornListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    public DragonHornListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHornBlow(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.GOAT_HORN) {
            return;
        }

        if (player.hasCooldown(Material.GOAT_HORN)) {
            return;
        }

        if (item.getItemMeta() instanceof MusicInstrumentMeta meta) {
            MusicInstrument instrument = meta.getInstrument();
            
            if (instrument != null) {

                var registry =  RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT);
                var namespacedKey = registry.getKey(instrument);
                if(namespacedKey == null){
                    return;
                }

                String variantName = namespacedKey.getKey();

                if (variantName.equals("feel_goat_horn")) {
                    if (DragonHornHelper.isDragonHorn(item)) {
                        int currentSouls = DragonHornHelper.getSouls(item);

                        if (currentSouls > 0) {
                            DragonHornHelper.setSouls(item, currentSouls - 1);


                            var spawnLoc = getTargetOrFront(player).getLocation().add(0,1,0);

                            player.playSound(spawnLoc,Sound.ENTITY_GENERIC_EXPLODE,1.0f,1.0f);

                            new CustomNautilus(player.getWorld(), plugin, player).spawn(spawnLoc);
                        }
                    }
                }
            }
        }
    }

    public @NotNull Block getTargetOrFront(@NotNull Player player) {
        Block target = player.getTargetBlockExact(5);


        if (target != null && target.getType() != Material.AIR) {
            return target;
        }

        Location loc = player.getEyeLocation();

        Vector direction = loc.getDirection().multiply(2);

        return loc.add(direction).getBlock();
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (clickedItem == null || clickedItem.getType() == Material.AIR || cursorItem.getType() == Material.AIR) {
            return;
        }

        if (DragonHornHelper.isDragonHorn(clickedItem) && DragonHornHelper.isSoulItem(cursorItem)) {
            event.setCancelled(true);

            int soulsToAdd = cursorItem.getAmount();

            DragonHornHelper.addSouls(clickedItem, soulsToAdd);

            event.getWhoClicked().setItemOnCursor(null);

            event.getWhoClicked().getWorld().playSound(
                    event.getWhoClicked().getLocation(),
                    Sound.ITEM_SPYGLASS_USE, 1.0f, 1.5f
            );
        }
    }
}