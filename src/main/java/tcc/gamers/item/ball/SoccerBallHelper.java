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

public class SoccerBallHelper {

    @NotNull
    public static ItemStack createSoccerBall() {
        ItemStack item = new ItemStack(Material.POPPED_CHORUS_FRUIT);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        NamespacedKey key = TCCNameSpacedKeys.SOCCER_BALL.getNamespacedKey();

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        meta.displayName(Component.text("'Bola de Futbol'")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Sirve para colocar una pelota", NamedTextColor.GRAY));

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }

    public static boolean isSoccerBall(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return false;
        if(item.isEmpty()) return false;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.SOCCER_BALL.getNamespacedKey();

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}