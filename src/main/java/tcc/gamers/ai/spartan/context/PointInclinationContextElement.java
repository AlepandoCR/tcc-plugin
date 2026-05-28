package tcc.gamers.ai.spartan.context;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Optional;
import java.util.function.Supplier;

public class PointInclinationContextElement extends AbstractEntityPointContextElement {

    public PointInclinationContextElement(
            @NotNull Supplier<@NotNull Optional<Entity>> entitySupplier,
            @NotNull Supplier<@NotNull Optional<Location>> pointSupplier
    ) {
        super(entitySupplier, pointSupplier);
    }

    public double getInclinationRadians() {
        return rawValue;
    }

    @Override
    protected double calculateRawValue(Location entityLoc, Location targetLoc) {
        double dx = targetLoc.getX() - entityLoc.getX();
        double dy = targetLoc.getY() - entityLoc.getY();
        double dz = targetLoc.getZ() - entityLoc.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        return Math.atan2(dy, horizontalDistance);
    }

    @Override
    public @Range(from = -1L, to = 1L) double getValue() {
        double normalized = rawValue / (Math.PI / 2.0);
        
        return Math.max(-1.0, Math.min(1.0, normalized));
    }

    @Override
    protected @NotNull String getElementPrefix() {
        return "point_inclination_context_element";
    }
}