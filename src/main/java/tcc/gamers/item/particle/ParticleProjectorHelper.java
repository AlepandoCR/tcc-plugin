package tcc.gamers.item.particle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

import java.util.ArrayList;
import java.util.List;

public class ParticleProjectorHelper {

    @NotNull
    public static ItemStack createParticleProjector(
            double radius,
            int amount,
            @NotNull String particleName
    ) {
        ItemStack item = new ItemStack(Material.FLOW_BANNER_PATTERN);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            NamespacedKey keyProjector = TCCNameSpacedKeys.PARTICLE_PROJECTOR.getNamespacedKey();
            meta.getPersistentDataContainer().set(keyProjector, PersistentDataType.BYTE, (byte) 1);

            meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_RADIUS.getNamespacedKey(), PersistentDataType.DOUBLE, radius);
            meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_AMOUNT.getNamespacedKey(), PersistentDataType.INTEGER, amount);
            meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_NAME.getNamespacedKey(), PersistentDataType.STRING, particleName);

            item.setItemMeta(meta);
        }

        updateVisuals(item, radius, amount, particleName);
        return item;
    }

    public static boolean isParticleProjector(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = TCCNameSpacedKeys.PARTICLE_PROJECTOR.getNamespacedKey();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public static double getRadius(@NotNull ItemStack item) {
        if (!isParticleProjector(item)) return 0.0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(
                TCCNameSpacedKeys.PROJECTOR_RADIUS.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0
        );
    }

    public static int getAmount(@NotNull ItemStack item) {
        if (!isParticleProjector(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(
                TCCNameSpacedKeys.PROJECTOR_AMOUNT.getNamespacedKey(), PersistentDataType.INTEGER, 0
        );
    }

    @NotNull
    public static String getParticleName(@NotNull ItemStack item) {
        if (!isParticleProjector(item)) return "FLAME";
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(
                TCCNameSpacedKeys.PROJECTOR_NAME.getNamespacedKey(), PersistentDataType.STRING, "FLAME"
        );
    }

    public static void updateSettings(
            @NotNull ItemStack item,
            double radius,
            int amount,
            @NotNull String particleName
    ) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_RADIUS.getNamespacedKey(), PersistentDataType.DOUBLE, radius);
        meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_AMOUNT.getNamespacedKey(), PersistentDataType.INTEGER, amount);
        meta.getPersistentDataContainer().set(TCCNameSpacedKeys.PROJECTOR_NAME.getNamespacedKey(), PersistentDataType.STRING, particleName);
        item.setItemMeta(meta);

        updateVisuals(item, radius, amount, particleName);
    }

    private static void updateVisuals(
            @NotNull ItemStack item,
            double radius,
            int amount,
            @NotNull String particleName
    ) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.displayName(Component.text("Proyector de Partículas")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Un dispositivo de ingeniería capaz", NamedTextColor.GRAY));
        lore.add(Component.text("de moldear particulas", NamedTextColor.GRAY));
        lore.add(Component.empty());

        lore.add(Component.text("✨ Configuración Actual:", NamedTextColor.GOLD));
        lore.add(Component.text("  • Partícula: ", NamedTextColor.GRAY)
                .append(Component.text(particleName.toUpperCase(), NamedTextColor.YELLOW)));
        lore.add(Component.text("  • Radio: ", NamedTextColor.GRAY)
                .append(Component.text(radius, NamedTextColor.AQUA)));
        lore.add(Component.text("  • Cantidad: ", NamedTextColor.GRAY)
                .append(Component.text(amount, NamedTextColor.AQUA)));


        meta.lore(lore);
        item.setItemMeta(meta);
    }
}