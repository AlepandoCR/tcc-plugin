package tcc.gamers.item.ball;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

import java.util.ArrayList;
import java.util.List;

public class SoccerCleatsHelper {

    @NotNull
    public static ItemStack createSoccerCleats() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        NamespacedKey key = TCCNameSpacedKeys.SOCCER_CLEATS.getNamespacedKey();

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        meta.displayName(Component.text("'Tacos de Fútbol'")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Botas de cuero con suela,", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("diseñadas para tener el mejor", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("agarre en la cancha.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static boolean isSoccerCleats(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return false;
        if(item.isEmpty()) return false;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.SOCCER_CLEATS.getNamespacedKey();

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }


    public static boolean playerHasCleats(@NotNull Player player){
        var item = player.getInventory().getBoots();
        return isSoccerCleats(item);
    }
}