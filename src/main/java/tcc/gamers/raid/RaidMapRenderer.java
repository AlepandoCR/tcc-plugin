package tcc.gamers.raid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Vault;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class RaidMapRenderer extends MapRenderer {

    private static final int BLOCKS_PER_PIXEL = 16;

    private boolean rendered = false;

    private final @NotNull Raid raid;

    public RaidMapRenderer(@NotNull Raid raid) {
        this.raid = raid;
    }

    public @NotNull ItemStack create() {

        var target = raid.getCenter();
        MapView view = Bukkit.createMap(target.getWorld());
        view.setScale(MapView.Scale.FARTHEST);
        view.setCenterX(target.getBlockX());
        view.setCenterZ(target.getBlockZ());
        view.setTrackingPosition(false);
        view.getRenderers().clear();
        view.addRenderer(new RaidMapRenderer(raid));

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);

        meta.displayName(
                Component.text("✦ Mapa de Raid", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.BOLD, false))
                        .append(Component.text(raid.getIdentifier(), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, false))
        );

        int x = target.getBlockX();
        int y = target.getBlockY();
        int z = target.getBlockZ();
        int mobCount   = raid.getRaidMobs().size();
        int minPlayers = raid.getRequiredPlayersAmount();

        meta.lore(List.of(
                Component.empty(),
                Component.text("  Ubicación", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("  X: ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(x, NamedTextColor.WHITE)),
                Component.text("  Y: ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(y, NamedTextColor.WHITE)),
                Component.text("  Z: ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(z, NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("  Mobs: ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(mobCount, NamedTextColor.RED)),
                Component.text("  Jugadores recomendados: ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(minPlayers, NamedTextColor.AQUA)),
                Component.empty(),
                Component.text("  La ", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, true)
                        .append(Component.text("X", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" marca el destino.", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, true))
        ));

        map.setItemMeta(meta);
        return map;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (rendered) return;

        World world = map.getWorld();
        if (world == null) return;

        var nmsWorld = ((CraftWorld) world).getHandle();

        int centerX = map.getCenterX();
        int centerZ = map.getCenterZ();

        int startX = centerX - (64 * BLOCKS_PER_PIXEL);
        int startZ = centerZ - (64 * BLOCKS_PER_PIXEL);

        for (int px = 0; px < 128; px++) {
            for (int pz = 0; pz < 128; pz++) {
                int blockX = startX + (px * BLOCKS_PER_PIXEL);
                int blockZ = startZ + (pz * BLOCKS_PER_PIXEL);

                int highY      = world.getHighestBlockYAt(blockX, blockZ);
                int highYNorth = world.getHighestBlockYAt(blockX, blockZ - BLOCKS_PER_PIXEL);

                MapColor.Brightness brightness;
                if (highY > highYNorth) {
                    brightness = MapColor.Brightness.HIGH;
                } else if (highY < highYNorth) {
                    brightness = MapColor.Brightness.LOW;
                } else {
                    brightness = MapColor.Brightness.NORMAL;
                }

                var nmsBlockPos = new BlockPos(blockX, highY, blockZ);
                var nmsBlockState = nmsWorld.getBlockState(nmsBlockPos);
                MapColor mapColor = nmsBlockState.getMapColor(nmsWorld, nmsBlockPos);

                int rgb = mapColor.calculateARGBColor(brightness);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;

                canvas.setPixelColor(px, pz, new Color(r,g,b));
            }
        }

        MapCursorCollection cursors = canvas.getCursors();
        cursors.addCursor(new MapCursor(
                (byte) 0,
                (byte) 0,
                (byte) 8,
                MapCursor.Type.TARGET_X,
                true
        ));

        rendered = true;
    }
}