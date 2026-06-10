package tcc.gamers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChestBuilder {

    private static final int CHEST_SIZE       = 27;
    private static final int ITEMS_PER_CHEST  = CHEST_SIZE - 1;
    private static final int OVERFLOW_SLOT    = CHEST_SIZE - 1;

    private ChestBuilder() {}

    @NotNull
    public static ItemStack pack(@NotNull List<ItemStack> items, int depth) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot pack an empty item list into a chest");
        }

        ItemStack chestItem = new ItemStack(Material.CHEST);
        BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();

        Chest chestState = (Chest) meta.getBlockState();
        var inventory = chestState.getInventory();

        List<ItemStack> page     = items.subList(0, Math.min(ITEMS_PER_CHEST, items.size()));
        List<ItemStack> overflow = items.size() > ITEMS_PER_CHEST
                ? items.subList(ITEMS_PER_CHEST, items.size())
                : List.of();

        for (int i = 0; i < page.size(); i++) {
            inventory.setItem(i, page.get(i));
        }

        if (!overflow.isEmpty()) {
            inventory.setItem(OVERFLOW_SLOT, pack(overflow, depth + 1));
        }

        meta.displayName(
                Component.text("Mapas de Raids")
                        .color(NamedTextColor.GOLD)
                        .append(depth > 0
                                ? Component.text(" (cont. " + depth + ")").color(NamedTextColor.GRAY)
                                : Component.empty()
                        )
        );

        meta.setBlockState(chestState);
        chestItem.setItemMeta(meta);
        return chestItem;
    }


    @NotNull
    public static ItemStack pack(@NotNull List<ItemStack> items) {
        return pack(items, 0);
    }
}