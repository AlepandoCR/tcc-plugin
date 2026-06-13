package tcc.gamers.ai.controller;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.nms.nautilus.AutonomousDragonNautilus;

import java.util.UUID;

/**
 * Per-dragon helper responsible for:
 * <ol>
 *   <li>Applying flight actions to the NMS entity (rotation + applyFlight).</li>
 *   <li>Computing the per-tick reward signal.</li>
 *   <li>Logging a short per-interval line for per-dragon debugging.</li>
 *   <li>Forwarding all aggregated data to the session-level
 *       {@link DragonModelMetrics} owned by the manager.</li>
 * </ol>
 *
 */
public class DragonFlightMetricsHelper {

    private final @NotNull AutonomousDragonNautilus dragon;
    private final @NotNull ComponentLogger logger;

    /**
     * The session-level aggregator. Every per-tick data point recorded here
     * is also forwarded there so the manager-level report reflects ALL dragons.
     */
    private final @NotNull DragonModelMetrics modelMetrics;

    private static final double MAX_RAYTRACE_DISTANCE = 40.0;
    private static final float  MAX_DEGREES_PER_TICK  = 15.0f;
    private static final int    METRICS_INTERVAL      = 200;

    private static final double GAMMA                = 0.99;
    private static final double PROGRESS_WEIGHT      = 5.0;
    private static final double APPROACH_WEIGHT       = 2.0;
    private static final double BOUNCE_PENALTY_WEIGHT = 5.0;
    private static final double ALIGNMENT_WEIGHT      = 2.0;
    private static final double PITCH_ERROR_WEIGHT    = 1.0;
    private static final double ARRIVAL_BONUS        = 500.0;
    private static final double ARRIVAL_RADIUS       = 3.0;
    private static final double OBSTACLE_PENALTY     = 15.0;

    private int    tickCounter            = 0;
    private double accumulatedReward      = 0.0;
    private double accumulatedYawDelta    = 0.0;
    private double accumulatedPitchDelta  = 0.0;
    private double accumulatedBounceRatio = 0.0;

    private double lastDistance = -1.0;

    private double currentYawVelocity   = 0.0;
    private double currentPitchVelocity = 0.0;

    public DragonFlightMetricsHelper(
            @NotNull AutonomousDragonNautilus dragon,
            @NotNull ComponentLogger logger,
            @NotNull DragonModelMetrics modelMetrics
    ) {
        this.dragon       = dragon;
        this.logger       = logger;
        this.modelMetrics = modelMetrics;
        modelMetrics.notifyDragonSpawned();
    }

    public void applyAndTrackActions(double rawYawDelta, double rawPitchDelta) {
        double yawDelta   = Math.tanh(rawYawDelta);
        double pitchDelta = Math.tanh(rawPitchDelta);

        currentYawVelocity   = currentYawVelocity   * 0.5 + yawDelta   * 0.5;
        currentPitchVelocity = currentPitchVelocity * 0.5 + pitchDelta * 0.5;

        float deltaYaw   = (float) (currentYawVelocity   * MAX_DEGREES_PER_TICK);
        float deltaPitch = (float) (currentPitchVelocity * MAX_DEGREES_PER_TICK);

        accumulatedYawDelta   += Math.abs(deltaYaw);
        accumulatedPitchDelta += Math.abs(deltaPitch);

        float newYaw   = dragon.getYRot() + deltaYaw;
        float newPitch = Mth.clamp(dragon.getXRot() + deltaPitch, -90.0f, 90.0f);

        dragon.setYRot(newYaw);
        dragon.setXRot(newPitch);

        float yawRad   = newYaw   * (float) (Math.PI / 180.0);
        float pitchRad = newPitch * (float) (Math.PI / 180.0);
        Vec3 lookAngle = new Vec3(
                -Math.sin(yawRad)  * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad)  * Math.cos(pitchRad)
        );

        dragon.applyFlight(lookAngle, newPitch);
    }

    /**
     * Computes the full reward for this tick and forwards data to
     * {@link DragonModelMetrics}.
     *
     * <h3>Progress shaping</h3>
     * Uses plain {@code deltaProgress = lastDist - currentDist}
     */
    public double calculateAndTrackReward() {
        if (!dragon.hasTarget()) {
            this.lastDistance = -1.0;
            return 0.0;
        }

        Vec3 offset = dragon.getTargetOffset();
        double currentDist = offset.length();
        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 idealDir = offset.normalize();
        Vec3 lookAngle = dragon.getLookAngle().normalize();

        if (this.lastDistance < 0) this.lastDistance = currentDist;

        double reward = 0.0;

        double deltaProgress = this.lastDistance - currentDist;
        reward += deltaProgress * PROGRESS_WEIGHT;

        double verticalProgress = velocity.y * idealDir.y;
        double verticalMagnitude = Math.abs(velocity.y);
        double bounceRatio = 0.0;
        boolean isBouncing = false;

        if (verticalMagnitude > 0.05 && verticalProgress < 0.01) {
            bounceRatio = verticalMagnitude / (velocity.length() + 0.001);
            if (bounceRatio > 0.4) {
                reward  -= bounceRatio * BOUNCE_PENALTY_WEIGHT;
                isBouncing = true;
            }
        }

        double approachVelocity = velocity.dot(idealDir);
        reward += Math.max(0.0, approachVelocity) * APPROACH_WEIGHT;

        double pitchError = idealDir.y - lookAngle.y;
        reward -= Math.abs(pitchError) * PITCH_ERROR_WEIGHT;

        double alignment = lookAngle.dot(idealDir);
        reward += alignment * ALIGNMENT_WEIGHT;

        boolean isArrival = currentDist < ARRIVAL_RADIUS;
        if (isArrival) reward += ARRIVAL_BONUS;

        double obstacleRatio = getObstacleDistanceRatio();
        if (obstacleRatio < 0.2) reward -= OBSTACLE_PENALTY;


        reward -= 0.05;

        this.lastDistance = currentDist;
        accumulatedReward += reward;
        accumulatedBounceRatio += bounceRatio;
        tickCounter++;

        modelMetrics.record(reward, bounceRatio, alignment, isBouncing, isArrival);

        if (tickCounter >= METRICS_INTERVAL) {
            dumpIntervalLine();
            resetInterval();
        }

        return reward;
    }

    private void dumpIntervalLine() {
        double avgReward = accumulatedReward      / tickCounter;
        double avgYaw = accumulatedYawDelta    / tickCounter;
        double avgPitch = accumulatedPitchDelta  / tickCounter;
        double avgBounce = accumulatedBounceRatio / tickCounter;

        String warn = avgBounce > 0.4 ? " ⚠ BOUNCE" : "";

        logger.info(
                Component.text(
                        String.format(
                            "[Dragon %s] Avg Reward: %.3f | Yaw: %.2f°/t | Pitch: %.2f°/t | Bounce: %.2f%s",
                            shortUuid(),
                            avgReward,
                            avgYaw,
                            avgPitch,
                            avgBounce,
                            warn
                        )
                )
        );
    }

    private void resetInterval() {
        tickCounter = 0;
        accumulatedReward = 0.0;
        accumulatedYawDelta = 0.0;
        accumulatedPitchDelta = 0.0;
        accumulatedBounceRatio = 0.0;
    }

    /** Called by the session when this dragon is replaced due to timeout. */
    public void notifyTimeout()    { modelMetrics.notifyTimeout(); }

    /** Called by the session when this dragon is replaced due to out-of-bounds. */
    public void notifyOutOfBounds(){ modelMetrics.notifyOutOfBounds(); }

    public void resetDistanceTracking() { this.lastDistance = -1.0; }

    public @NotNull UUID getDragonUUID() { return dragon.getUUID(); }

    public double getObstacleDistanceRatio() {
        World world = dragon.getBukkitEntity().getWorld();
        Vec3  pos   = dragon.position();
        Vec3  look  = dragon.getLookAngle().normalize();

        Vector   direction = new Vector(look.x, look.y, look.z);
        Location startLoc  = new Location(world, pos.x, pos.y, pos.z);

        RayTraceResult result = world.rayTraceBlocks(startLoc, direction, MAX_RAYTRACE_DISTANCE);

        if (result != null && result.getHitBlock() != null) {
            double actualDistance = startLoc.distance(result.getHitBlock().getLocation());
            return Math.max(0.0, actualDistance / MAX_RAYTRACE_DISTANCE);
        }
        return 1.0;
    }

    @NotNull
    private String shortUuid() { return dragon.getUUID().toString().substring(0, 8); }
}

