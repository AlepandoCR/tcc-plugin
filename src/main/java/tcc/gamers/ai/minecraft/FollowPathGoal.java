package tcc.gamers.ai.minecraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.SpeedController;
import tcc.gamers.nms.horses.CustomSpartanHorse;
import tcc.gamers.util.Path;
import tcc.gamers.util.PathMode;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class FollowPathGoal<EntityType extends CustomSpartanHorse<?>> implements BehaviorControl<EntityType> {

    private final Path path;
    private Location currentLocation;
    private Location targetPoint;
    private final SpeedController speedController;
    private final Logger logger;

    private static final double ARRIVAL_DISTANCE          = 1.5;
    private static final double ORBIT_SKIP_DISTANCE       = 1.8;  // was 2.6 — tighter; only skip when truly orbiting
    private static final int    STUCK_REISSUE_TICKS       = 15;   // was 10 — less hair-trigger
    private static final int    MAX_REISSUE_ATTEMPTS      = 8;    // was 5 — more patience before skipping
    private static final int    ORBIT_MIN_ATTEMPTS        = 5;    // was 3 — require more evidence before orbit-skip
    private static final int    MOVE_TO_COOLDOWN_TICKS    = 8;
    private static final double SPEED_REISSUE_DELTA       = 0.12;
    private static final double STUCK_PROGRESS_TOLERANCE  = 0.05; // was 0.01 — real zero-progress band

    private double lastDistanceToTarget = -1.0;
    private int    stuckTicks           = 0;
    private int    reissueAttempts      = 0;
    private long   lastMoveToTick       = Long.MIN_VALUE;
    private double lastIssuedSpeed      = Double.NaN;

    public FollowPathGoal(
            @NotNull Path path,
            @NotNull PathMode pathMode,
            @NotNull SpeedController speedController,
            @NotNull TCCPlugin plugin
    ) {
        this.path = path;
        this.speedController = speedController;
        this.logger = plugin.getLogger();
        this.path.setMode(pathMode);
    }

    @Override
    public @NotNull Behavior.Status getStatus() {
        return Behavior.Status.RUNNING;
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public boolean tryStart(
            @NotNull ServerLevel serverLevel,
            @NotNull EntityType customHorse,
            long tick
    ) {
        return this.path.peekNext() != null;
    }

    @Override
    public void tickOrStop(
            @NotNull ServerLevel serverLevel,
            @NotNull EntityType customHorse,
            long tick
    ) {
        // ── Phase 1: acquire a target ─────────────────────────────────────────
        if (targetPoint == null) {
            targetPoint = this.path.peekNext();
            if (targetPoint == null) {
                return; // path exhausted
            }
            resetProgressTracking();
            return;
        }

        // ── Phase 2: check arrival ────────────────────────────────────────────
        var horsePos = customHorse.getBukkitEntity().getLocation();
        double distanceToTarget = horsePos.distance(targetPoint);

        if (distanceToTarget < ARRIVAL_DISTANCE) {
            logger.info("[tcc-plugin][FollowPathGoal] arrived -> dist=" + distanceToTarget
                    + " horse=" + customHorse.getUUID() + " point=" + targetPoint);

            this.currentLocation = this.path.next(); // consume arrived point

            // If the queue is now empty this was the final waypoint, despawn.
            if (this.path.peekNext() == null) {
                logger.info("[tcc-plugin][FollowPathGoal] path exhausted, despawning horse=" + customHorse.getUUID());
                customHorse.despawnHorse();
                return;
            }

            this.targetPoint = null;
            return;
        }

        // ── Phase 3: maintain navigation toward current target ────────────────
        double speed = speedController.getSpeed();

        // Track progress; only flag stuck when there is genuinely zero forward movement
        if (lastDistanceToTarget >= 0.0) {
            if (distanceToTarget >= lastDistanceToTarget - STUCK_PROGRESS_TOLERANCE) {
                stuckTicks++;
            } else {
                stuckTicks = 0;
            }
        }
        lastDistanceToTarget = distanceToTarget;

        // Reissue when the navigator has stopped or speed changed meaningfully.
        // TTL-based reissue has been removed: forcing the pathfinder to replan
        // mid-navigation caused oscillation more often than it helped.
        boolean navDone     = customHorse.getNavigation().isDone();
        boolean speedChanged = Double.isNaN(lastIssuedSpeed)
                || Math.abs(lastIssuedSpeed - speed) >= SPEED_REISSUE_DELTA;

        if (navDone || speedChanged) {
            issueMoveTo(customHorse, speed, tick, false);
        }

        //Phase 4: stuck recovery
        if (stuckTicks >= STUCK_REISSUE_TICKS) {
            reissueAttempts++;
            logger.warning("[tcc-plugin][FollowPathGoal] stuck, reissuing moveTo"
                    + " attempt=" + reissueAttempts + " horse=" + customHorse.getUUID());

            boolean moved = issueMoveTo(customHorse, speed, tick, true);
            stuckTicks = 0;

            // Skip only when there is strong evidence the waypoint is unreachable:
            //   a) the navigator repeatedly fails outright, OR
            //   b) the horse is clearly orbiting very close to the target
            boolean hardFail   = !moved || reissueAttempts >= MAX_REISSUE_ATTEMPTS;
            boolean orbitingTarget = distanceToTarget <= ORBIT_SKIP_DISTANCE
                    && reissueAttempts >= ORBIT_MIN_ATTEMPTS;

            if (hardFail || orbitingTarget) {
                logger.warning("[tcc-plugin][FollowPathGoal] skipping unreachable waypoint"
                        + " horse=" + customHorse.getUUID() + " point=" + targetPoint);
                this.currentLocation = this.path.next(); // consume unreachable point
                this.targetPoint = null; // Phase 1 will pick next point tick
            }
        }
    }

    private boolean issueMoveTo(@NotNull EntityType customHorse, double speed, long tick, boolean force) {
        if (targetPoint == null) {
            return false;
        }
        if (!force && tick - lastMoveToTick < MOVE_TO_COOLDOWN_TICKS) {
            return true;
        }

        double targetX = targetPoint.x();
        double targetY = resolveWalkableY(customHorse, targetPoint);
        double targetZ = targetPoint.z();

        logger.fine("[tcc-plugin][FollowPathGoal] moveTo -> target=("
                + targetX + "," + targetY + "," + targetZ
                + ") speed=" + speed + " horse=" + customHorse.getUUID());

        boolean moved = customHorse.getNavigation().moveTo(targetX, targetY, targetZ, speed);

        lastMoveToTick  = tick;
        lastIssuedSpeed = speed;
        return moved;
    }

    /**
     * Resolves the correct walkable Y for a target location.
     * The NMS pathfinder works on integer block positions and resolves walkable
     * surfaces from there. Passing a fractional Y (e.g. 65.5 for a slab) causes
     * it to either fail path-finding or stop just short of the target.
     * Strategy:
     *   1. Try the block at floor(Y) — if it's solid and has a walkable shape
     *      (slab, stair, etc.) use its collision-box top as the surface Y.
     *   2. Fall back to the world's highest solid block at that XZ so vegetation
     *      (grass, flowers) is skipped, and we land on the block beneath.
     *   3. If the world query returns below the recorded Y (e.g. the waypoint is
     *      on an elevated platform), trust the recorded Y floored to integer —
     *      the pathfinder handles integer Y reliably.
     */
    private double resolveWalkableY(@NotNull EntityType customHorse, @NotNull Location target) {
        var world = customHorse.getBukkitEntity().getWorld();
        int blockX = target.getBlockX();
        int blockZ = target.getBlockZ();
        int recordedBlockY = target.getBlockY(); // floor of the recorded Y

        // Ask Bukkit for the highest solid block at this XZ column.
        // getHighestBlockYAt uses MOTION_BLOCKING which stops at solid blocks
        // and passable vegetation — exactly what we want.
        int surfaceY = world.getHighestBlockYAt(blockX, blockZ,
                org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES);

        // If the world surface is at or one block above the recorded position,
        // the waypoint is on terrain — use the world surface.
        if (surfaceY >= recordedBlockY - 1 && surfaceY <= recordedBlockY + 1) {
            // Check if the block at recordedBlockY is a slab or similar partial block
            // by reading its collision shape top via the block data.
            var blockAtRecorded = world.getBlockAt(blockX, recordedBlockY, blockZ);
            if (!blockAtRecorded.isPassable() && blockAtRecorded.getBoundingBox().getHeight() <= 0.5) {
                // It's a slab (or half-height block) — the walkable surface is Y + 0.5
                return recordedBlockY + 0.5;
            }
            return surfaceY; // full block surface
        }

        // The waypoint is on an elevated structure not detected by the column scan
        // (e.g. a bridge, a multi-floor building). Trust the recorded block Y —
        // integer Y is always valid for the NMS pathfinder.
        return recordedBlockY;
    }

    private void resetProgressTracking() {
        lastDistanceToTarget = -1.0;
        stuckTicks           = 0;
        reissueAttempts      = 0;
    }

    @Override
    public void doStop(
            @NotNull ServerLevel serverLevel,
            @NotNull CustomSpartanHorse customSpartanHorse,
            long tick
    ) {
        customSpartanHorse.getNavigation().stop();
        targetPoint    = null;
        currentLocation = null;
        lastIssuedSpeed = Double.NaN;
        lastMoveToTick  = Long.MIN_VALUE;
        resetProgressTracking();
    }

    @Override
    public @NotNull String debugString() {
        return "follow_path_goal";
    }

    public @NotNull Optional<Location> getCurrentPoint() {
        return Optional.ofNullable(currentLocation);
    }
}