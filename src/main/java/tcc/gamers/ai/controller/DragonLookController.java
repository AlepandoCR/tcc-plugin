package tcc.gamers.ai.controller;

import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.ai.queue.DragonQueueManager;
import tcc.gamers.ai.queue.DragonStateRequest;
import tcc.gamers.ai.spartan.action.reward.SpartanRewarder;
import tcc.gamers.nms.nautilus.AutonomousDragonNautilus;

public class DragonLookController implements SpartanRewarder, Controller {

    private final @NotNull AutonomousDragonNautilus dragon;
    private final @NotNull DragonQueueManager queueManager;
    private final @NotNull DragonFlightMetricsHelper metricsHelper;

    private static final double MAX_RAYTRACE_DISTANCE   = 40.0;
    private static final double MAX_EXPECTED_SPEED      = 3.0;
    private static final double ARRIVAL_THRESHOLD       = 4.0;

    /**
     * @param dragon       The NMS dragon entity this controller steers.
     * @param queueManager The shared brain to submit requests to.
     *                     Injected rather than constructed here
     */
    public DragonLookController(
            @NotNull AutonomousDragonNautilus dragon,
            @NotNull DragonQueueManager queueManager
    ) {
        this.dragon = dragon;
        this.queueManager = queueManager;
        // Pass the manager's DragonModelMetrics so all per-tick data is
        // forwarded to the session-level aggregator automatically.
        this.metricsHelper = new DragonFlightMetricsHelper(
                dragon,
                dragon.getPlugin().getComponentLogger(),
                queueManager.getModelMetrics()
        );
    }

    /**
     * <p>This method's only job is to sample the dragon's current sensory state
     * and submit it to the queue. It returns immediately — no model computation
     * happens here. The result arrives asynchronously (within the same tick) via
     * {@link #applyCalculatedActions(double, double)}.</p>
     */
    @Override
    public void tick() {
        if (!dragon.hasTarget()) return;
        if (!queueManager.isReady()) return;
        sampleAndSubmit();
    }


    /**
     * Computes all context values from the live NMS entity state and packages
     * them into an immutable {@link DragonStateRequest}, then submits to the queue.
     *
     * <p>All computation here is O(1) arithmetic on already-cached NMS field
     *
     * <p>All values are computed on the <strong>main server thread</strong>, where
     * NMS entity field access is safe. By the time the request reaches the queue,
     * it is a fully independent snapshot — the entity can mutate freely without
     * affecting the queued data.</p>
     */
    private void sampleAndSubmit() {
        Vec3 targetOffset = dragon.getTargetOffset();
        Vec3 velocity  = dragon.getDeltaMovement();
        Vec3 forward = dragon.getLookAngle().normalize();
        Vec3 idealDirection = targetOffset.normalize();

        double targetDirX = idealDirection.x;
        double targetDirY = idealDirection.y;
        double targetDirZ = idealDirection.z;

        double velX = Math.max(-1.0, Math.min(1.0, velocity.x / MAX_EXPECTED_SPEED));
        double velY = Math.max(-1.0, Math.min(1.0, velocity.y / MAX_EXPECTED_SPEED));
        double velZ = Math.max(-1.0, Math.min(1.0, velocity.z / MAX_EXPECTED_SPEED));

        double obstacleDistanceRatio = metricsHelper.getObstacleDistanceRatio();
        double targetDistance = Math.min(1.0, targetOffset.length() / 100.0);

        double cross  = forward.x * idealDirection.z - forward.z * idealDirection.x;
        double yawError = Math.max(-1.0, Math.min(1.0, cross));

        double pitchDiff = idealDirection.y - forward.y;
        double pitchError = Math.max(-1.0, Math.min(1.0, pitchDiff));

        queueManager.submit(DragonStateRequest.of(
                targetDirX, targetDirY, targetDirZ,
                velX, velY, velZ,
                obstacleDistanceRatio,
                targetDistance,
                yawError,
                pitchError,
                this
        ));
    }


    /**
     * Called by {@link DragonQueueManager#processTick()} on the main server thread
     * after the shared model has computed this dragon's action for the current tick.
     *
     * @param yawDelta   Raw actor output for yaw,   range [-1.0, 1.0] (Tanh-squashed).
     * @param pitchDelta Raw actor output for pitch, range [-1.0, 1.0] (Tanh-squashed).
     */
    public void applyCalculatedActions(double yawDelta, double pitchDelta) {
        if (!dragon.hasTarget()) return;
        metricsHelper.applyAndTrackActions(yawDelta, pitchDelta);
    }


    public boolean hasReachedTarget() {
        if (!dragon.hasTarget()) return false;
        return dragon.getTargetOffset().length() < ARRIVAL_THRESHOLD;
    }

    @Override
    public double reward() {
        return metricsHelper.calculateAndTrackReward();
    }

    public @NotNull Vec3 getLookAngle() {
        return this.dragon.getLookAngle();
    }

    /**
     * Performs a Paper API block raycast to find the nearest obstacle in the
     * looking direction.
     *
     * @return A normalized ratio [0.0, 1.0]: 1.0 = clear ahead, 0.0 = imminent collision.
     */
    private double getObstacleDistanceRatio() {
        World world = dragon.getBukkitEntity().getWorld();
        Vec3  pos   = dragon.position();
        Vec3  look  = getLookAngle().normalize();

        Vector   direction = new Vector(look.x, look.y, look.z);
        Location startLoc  = new Location(world, pos.x, pos.y, pos.z);

        RayTraceResult result = world.rayTraceBlocks(startLoc, direction, MAX_RAYTRACE_DISTANCE);

        if (result != null && result.getHitBlock() != null) {
            double actualDistance = startLoc.distance(result.getHitBlock().getLocation());
            return Math.max(0.0, actualDistance / MAX_RAYTRACE_DISTANCE);
        }

        return 1.0; // no obstacle detected
    }
}