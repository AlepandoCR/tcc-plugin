package tcc.gamers.area;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

public class Area {

    private final @NotNull Location center;
    private final @NotNull BoundingBox boundingBox;
    private final @NotNull String identifier;

    private final double xRadius;
    private final double yRadius;
    private final double zRadius;

    public Area(
            @NotNull Location center,
            @NotNull String identifier,
            double xRadius,
            double yRadius,
            double zRadius
    ) {
        this.center = center;
        this.identifier = identifier;
        this.xRadius = xRadius;
        this.yRadius = yRadius;
        this.zRadius = zRadius;
        boundingBox = BoundingBox.of(center, xRadius + 1, yRadius + 1, zRadius + 1); // + 1 on all to get the expected "contains" result
    }

    public @NotNull Location getCenter() {
        return center;
    }

    public double getXRadius() {
        return xRadius;
    }

    public double getYRadius() {
        return yRadius;
    }

    public double getZRadius() {
        return zRadius;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public boolean contains(@NotNull Entity entity) {
        return boundingBox.contains(entity.getBoundingBox());
    }

    public void visualize(@NotNull Player player) {
        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();

        double minX = cx - Math.max(0, this.xRadius);
        double maxX = cx + Math.max(0, this.xRadius);

        double minY = cy - Math.max(0, this.yRadius);
        double maxY = cy + Math.max(0, this.yRadius);

        double minZ = cz - Math.max(0, this.zRadius);
        double maxZ = cz + Math.max(0, this.zRadius);

        double step = 4.5;

        for (double x = minX; x <= maxX; x += step) {
            spawnEdgeParticle(player, x, minY, minZ);
            spawnEdgeParticle(player, x, minY, maxZ);
            spawnEdgeParticle(player, x, maxY, minZ);
            spawnEdgeParticle(player, x, maxY, maxZ);
        }

        for (double y = minY; y <= maxY; y += step) {
            spawnEdgeParticle(player, minX, y, minZ);
            spawnEdgeParticle(player, minX, y, maxZ);
            spawnEdgeParticle(player, maxX, y, minZ);
            spawnEdgeParticle(player, maxX, y, maxZ);
        }

        for (double z = minZ; z <= maxZ; z += step) {
            spawnEdgeParticle(player, minX, minY, z);
            spawnEdgeParticle(player, minX, maxY, z);
            spawnEdgeParticle(player, maxX, minY, z);
            spawnEdgeParticle(player, maxX, maxY, z);
        }
    }

    private void spawnEdgeParticle(
            @NotNull Player player,
            double x,
            double y,
            double z
    ) {
        player.spawnParticle(
                Particle.GLOW_SQUID_INK,
                x,
                y,
                z,
                1,
                0,
                0,
                0,
                0
        );
    }
}
