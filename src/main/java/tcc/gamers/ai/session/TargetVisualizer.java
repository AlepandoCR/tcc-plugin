package tcc.gamers.ai.session;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TargetVisualizer {

    private Material displayMaterial;
    private float scale;
    private final Map<UUID, BlockDisplay> activeDisplays = new HashMap<>();

    public TargetVisualizer(@NotNull Material defaultMaterial, float defaultScale) {
        this.displayMaterial = defaultMaterial;
        this.scale = defaultScale;
    }

    public void setConfig(@NotNull Material material, float scale) {
        this.displayMaterial = material;
        this.scale = scale;
    }

    public void showTarget(@NotNull UUID dragonId, @NotNull Location target) {
        BlockDisplay display = activeDisplays.get(dragonId);

        if (display == null || !display.isValid()) {
            display = (BlockDisplay) target.getWorld().spawnEntity(target, EntityType.BLOCK_DISPLAY);
            display.setBlock(displayMaterial.createBlockData());

            float offset = -(scale / 2.0f); 
            display.setTransformation(new Transformation(
                    new Vector3f(offset, offset, offset),
                    new AxisAngle4f(),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f()
            ));
            
            activeDisplays.put(dragonId, display);
        } else {
            display.teleport(target);
        }
    }

    public void clearTarget(@NotNull UUID dragonId) {
        BlockDisplay display = activeDisplays.remove(dragonId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void clearAll() {
        for (BlockDisplay display : activeDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }
}