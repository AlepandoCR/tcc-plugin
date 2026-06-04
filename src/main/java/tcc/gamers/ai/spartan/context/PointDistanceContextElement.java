package tcc.gamers.ai.spartan.context;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.spartan.api.engine.context.element.SpartanSingleContextElement;

import java.util.Optional;
import java.util.function.Supplier;

public class PointDistanceContextElement extends SpartanSingleContextElement {

    private double value;

    private final Supplier<Optional<Entity>> entitySupplier;

    private final Supplier<Optional<Location>> pointSupplier;

    public PointDistanceContextElement(
            @NotNull Supplier<Optional<Entity>> entitySupplier,
            @NotNull Supplier<Optional<Location>> pointSupplier
    ) {
        this.value = 0;
        this.entitySupplier = entitySupplier;
        this.pointSupplier = pointSupplier;
    }

    /**
     * Returns the last-measured raw distance between the entity and the point.
     * This value is updated during {@link #tick()} and defaults to 0 when
     * no measurement has been made yet.
     */
    public double getDistance() {
        return value;
    }

    @Override
    public @Range(from = -1L, to = 1L) double getValue() {
        //normalize to -1 1
        return Math.max(-1, Math.min(1, value));

    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public void tick() {
        var entityOpt =  entitySupplier.get();
        if (entityOpt.isPresent()) {
            var entity = entityOpt.get();

            var locationOpt = pointSupplier.get();
            locationOpt.ifPresent(location ->
                    value = entity
                            .getLocation()
                            .distance(location)
            );
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        var entityId = "n/a";

        if (entitySupplier.get().isPresent()) {
            entityId = entitySupplier
                    .get()
                    .get()
                    .getUniqueId()
                    .toString();
        }

        return "point_distance_context_element_entityid_" + entityId;
    }
}
