package tcc.gamers.item.dragon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

import java.util.ArrayList;
import java.util.List;

public class DragonHornHelper {

    @NotNull
    public static ItemStack createDragonHorn(int initialSouls) {
        ItemStack item = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof MusicInstrumentMeta instrumentMeta) {
            MusicInstrument feelHorn = Registry.INSTRUMENT.get(NamespacedKey.minecraft("feel_goat_horn"));
            if (feelHorn != null) {
                instrumentMeta.setInstrument(feelHorn);
            }
        }

        NamespacedKey key = TCCNameSpacedKeys.DRAGON_HORN.getNamespacedKey();
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, initialSouls);
        item.setItemMeta(meta);

        updateVisuals(item, initialSouls);
        return item;
    }

    public static boolean isDragonHorn(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.DRAGON_HORN.getNamespacedKey();
        
        return meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }


    public static int getSouls(ItemStack item) {
        if (!isDragonHorn(item)) return 0;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.DRAGON_HORN.getNamespacedKey();
        
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }


    public static void setSouls(ItemStack item, int souls) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.DRAGON_HORN.getNamespacedKey();

        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, souls);
        item.setItemMeta(meta);

        updateVisuals(item, souls);
    }


    public static void addSouls(ItemStack item, int amount) {
        int currentSouls = getSouls(item);
        setSouls(item, currentSouls + amount);
    }

    @NotNull
    public static ItemStack createSoulShard(int amount) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, amount);
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(
                TCCNameSpacedKeys.SOUL_ITEM.getNamespacedKey(),
                PersistentDataType.BYTE, (byte) 1
        );

        meta.displayName(Component.text("Fragmento de Alma").color(NamedTextColor.AQUA));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSoulItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                TCCNameSpacedKeys.SOUL_ITEM.getNamespacedKey(), PersistentDataType.BYTE
        );
    }


    private static void updateVisuals(@NotNull ItemStack item, int souls) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.displayName(Component.text("Cuerno de Dragón")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Un instrumento místico capaz de", NamedTextColor.GRAY));
        lore.add(Component.text("resonar a través de los cielos.", NamedTextColor.GRAY));
        lore.add(Component.empty());

        if (souls > 0) {
            lore.add(Component.text("🔥 Almas infundidas: ", NamedTextColor.AQUA)
                    .append(Component.text(souls, NamedTextColor.AQUA)));
        } else {
            lore.add(Component.text("❌ Sin almas ", NamedTextColor.RED)
                    .append(Component.text("(Necesita energía)", NamedTextColor.GRAY)));
        }

        lore.add(Component.empty());
        lore.add(Component.text("▶ ", NamedTextColor.GREEN)
                .append(Component.text("Click Derecho ", NamedTextColor.GREEN))
                .append(Component.text("para invocar", NamedTextColor.GRAY)));
        lore.add(Component.text("a tu montura (Consume 1 alma).", NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
    }
}