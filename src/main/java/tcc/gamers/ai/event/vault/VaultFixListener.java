package tcc.gamers.ai.event.vault;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Vault;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

public class VaultFixListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    public VaultFixListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerClickBlock(@NotNull PlayerInteractEvent event){
        var block = event.getClickedBlock();

        if (block != null && block.getType() == Material.VAULT && block.getState() instanceof Vault vault) {
            var pdc = vault.getPersistentDataContainer();

            if (!pdc.has(TCCNameSpacedKeys.FIXED_VAULT.getNamespacedKey(), PersistentDataType.BOOLEAN)) {
                var location = block.getLocation();

                var oldVaultData = (org.bukkit.block.data.type.Vault) vault.getBlockData();
                var facing = oldVaultData.getFacing();
                var isOminous = oldVaultData.isOminous();

                block.setType(Material.AIR);
                block.setType(Material.VAULT);

                var newVaultData = (org.bukkit.block.data.type.Vault) vault.getBlockData();

                newVaultData.setFacing(facing);
                newVaultData.setOminous(isOminous);
                newVaultData.setVaultState(org.bukkit.block.data.type.Vault.State.INACTIVE);
                vault.setBlockData(newVaultData);

                var registry = Registry.LOOT_TABLES;
                LootTables lootEnum = isOminous ? LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS : LootTables.TRIAL_CHAMBERS_REWARD;
                var lootTableEntry = registry.get(lootEnum.getKey());

                if (lootTableEntry != null) {
                    vault.setLootTable(lootTableEntry.getLootTable());


                    vault.getPersistentDataContainer().set(
                            TCCNameSpacedKeys.FIXED_VAULT.getNamespacedKey(),
                            PersistentDataType.BOOLEAN,
                            true
                    );

                    vault.update(true);

                    plugin.getComponentLogger().info(Component.text("Vault at " + location + " has been recreated and fixed").color(NamedTextColor.GREEN));
                }
            }
        }
    }
}