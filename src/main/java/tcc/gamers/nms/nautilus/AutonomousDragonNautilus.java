package tcc.gamers.nms.nautilus;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.DragonLookController;
import tcc.gamers.ai.queue.DragonQueueManager;

import java.util.Optional;


/**
 * An autonomous dragon entity steered by the Shared Brain architecture.
 */
public class AutonomousDragonNautilus extends AbstractDragonNautilus<AutonomousDragonNautilus> {


    private final @NotNull DragonQueueManager dragonQueueManager;

    private final @NotNull DragonLookController controller;

    private @Nullable Location targetLocation;

    private boolean started = false;

    /**
     * @param world             The Bukkit world to spawn in.
     * @param plugin            Plugin instance for API access.
     * @param dragonQueueManager The already-initialized shared brain.
     *                           Must not be null; must have had
     *                           {@link DragonQueueManager#initialize()} called before
     *                           this entity begins ticking.
     */
    public AutonomousDragonNautilus(
            @NotNull World world,
            @NotNull TCCPlugin plugin,
            @NotNull DragonQueueManager dragonQueueManager
    ) {
        super(world, plugin);
        this.dragonQueueManager = dragonQueueManager;
        this.setIsAutonomous(true);
        this.controller = new DragonLookController(this, dragonQueueManager);
    }

    public @NotNull TCCPlugin getPlugin(){
        return plugin;
    }

    @Override
    void onSpawn(@NotNull Location spawnLocation) {
        this.targetLocation = this.getBukkitNautilus().getLocation(); // start self as target
    }

    @Override
    void registerBrainActivities(@NotNull Brain<AutonomousDragonNautilus> brain) {
    }

    @Override
    void registerExtraGoals(@NotNull GoalSelector goalSelector) {
        // No goal-selector AI for this entity type
    }


    public void start() {
        this.started = true;
    }

    /** Stops the dragon from submitting state requests. In-flight requests already
     *  in the queue will still be processed and their callbacks will fire, but since
     *  the dragon is stopped the resulting applyCalculatedActions() call is guarded
     *  by the hasTarget() check inside the controller. */
    public void stop() {
        this.started = false;
    }

    public void setTargetLocation(@NotNull Location location) {
        this.targetLocation = location;
    }

    public @NotNull Optional<Location> getLocationTarget() {
        return Optional.ofNullable(targetLocation);
    }

    /**
     * Returns the 3D vector from this entity's current position to the target location.
     * Called by {@link DragonLookController} every tick to compute all directional
     * context values (alignment, yaw error, pitch error, target distance).
     *
     * @throws java.util.NoSuchElementException if called when no target is set.
     *         Callers must guard with {@link #hasTarget()} first.
     */
    public @NotNull Vec3 getTargetOffset() {
        var location = getLocationTarget().orElseThrow();
        Vec3 targetVec = new Vec3(
                location.getX(),
                location.getY(),
                location.getZ()
        );
        return targetVec.subtract(this.position());
    }

    /** @return {@code true} if the dragon is within the arrival radius of the target. */
    public boolean hasArrived() {
        return controller.hasReachedTarget();
    }

    /**
     * @return {@code true} if a target is set and it differs from the entity's
     *         current location. Guards all context sampling in the controller.
     */
    public boolean hasTarget() {
        if (targetLocation == null) return false;
        return !targetLocation.equals(this.getBukkitNautilus().getLocation());
    }

    @Override
    protected void tickController() {
        if (started) controller.tick();
    }
}