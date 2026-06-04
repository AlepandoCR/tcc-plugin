package tcc.gamers.ai.spartan.context;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.spartan.api.engine.context.element.SpartanSingleContextElement;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractEntityPointContextElement extends SpartanSingleContextElement {

    protected double rawValue;
    protected final Supplier<Optional<Entity>> entitySupplier;
    protected final Supplier<Optional<Location>> pointSupplier;

    public AbstractEntityPointContextElement(
            @NotNull Supplier<Optional<Entity>> entitySupplier,
            @NotNull Supplier<Optional<Location>> pointSupplier
    ) {
        this.rawValue = 0;
        this.entitySupplier = entitySupplier;
        this.pointSupplier = pointSupplier;
    }

    @Override
    public void tick() {
        var entityOpt = entitySupplier.get();
        var locationOpt = pointSupplier.get();

        if (entityOpt.isPresent() && locationOpt.isPresent()) {
            this.rawValue = calculateRawValue(entityOpt.get().getLocation(), locationOpt.get());
        }
    }

    protected abstract double calculateRawValue(Location entityLoc, Location targetLoc);

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public @NotNull String getIdentifier() {
        var entityId = "n/a";
        var entityOpt = entitySupplier.get();
        if (entityOpt.isPresent()) {
            entityId = entityOpt.get().getUniqueId().toString();
        }
        return getElementPrefix() + "_entityid_" + entityId;
    }

    protected abstract @NotNull String getElementPrefix();
}