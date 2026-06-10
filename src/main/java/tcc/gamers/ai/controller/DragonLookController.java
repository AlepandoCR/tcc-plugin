package tcc.gamers.ai.controller;

import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.spartan.api.engine.action.type.SpartanAction;
import org.spartan.api.engine.context.element.SpartanSingleContextElement;
import tcc.gamers.ai.spartan.action.DragonLookAction;
import tcc.gamers.ai.spartan.action.reward.SpartanRewarder;
import tcc.gamers.nms.nautilus.AutonomousDragonNautilus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public class DragonLookController implements SpartanRewarder, Controller {

    private final @NotNull AutonomousDragonNautilus dragon;

    private final DragonLookAction yawAction;
    private final DragonLookAction pitchAction;

    // reward Weights and constants
    // alignment weight intentionally lower than speed, we want the dragon to
    // learn to move toward the target, not just face it
    private static final double ALIGNMENT_WEIGHT = 3.0;
    private static final double SPEED_WEIGHT = 4.0;
    private static final double OBSTACLE_PENALTY_WEIGHT = 15.0;
    private static final double MAX_RAYTRACE_DISTANCE = 40.0;
    // max observed flight speed under the combo multiplier at baseline MOVEMENT_SPEED
    // used to normalize effective_velocity context and cap the speed reward signal
    private static final double MAX_EXPECTED_SPEED = 3.0;
    // arrival radius in blocks, below this, the episode is considered complete
    private static final double ARRIVAL_THRESHOLD = 3.0;

    public DragonLookController(@NotNull AutonomousDragonNautilus dragon) {
        this.dragon = dragon;
        this.yawAction = new DragonLookAction(this, DragonLookAction.Direction.YAW);
        this.pitchAction = new DragonLookAction(this, DragonLookAction.Direction.PITCH);
    }


    public boolean hasReachedTarget() {
        if (!dragon.hasTarget()) return false;
        return dragon.getTargetOffset().length() < ARRIVAL_THRESHOLD;
    }

    public @NotNull List<SpartanAction> getActions() {
        return List.of(yawAction, pitchAction);
    }

    @Override
    public void tick() {
        if (!dragon.hasTarget()) return;

        // max action (1.0) → 5° of rotation per tick
        // at 20 TPS that's 100degrees/s, for agile mid-flight correction
        float deltaYaw   = (float) (yawAction.getAngle()   / 180.0 * 5.0);
        float deltaPitch = (float) (pitchAction.getAngle() / 180.0 * 5.0);

        float newYaw   = dragon.getYRot() + deltaYaw;
        float newPitch = Math.clamp(dragon.getXRot() + deltaPitch, -90.0f, 90.0f);

        // operating in angle space (not vector space) ensures the action effect is
        // stationary: the same delta always produces the same angular rotation regardless
        // of current look direction
        float yawRad   = newYaw   * (float) (Math.PI / 180.0);
        float pitchRad = newPitch * (float) (Math.PI / 180.0);
        Vec3 lookAngle = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );

        // keep yRot/xRot in sync so getLookAngle() reads fresh state next tick
        dragon.setYRot(newYaw);
        dragon.setXRot(newPitch);

        dragon.applyFlight(lookAngle, newPitch);
    }

    @Override
    public double reward() {
        if (!dragon.hasTarget()) return 0.0;

        Vec3 offset = dragon.getTargetOffset();
        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 lookAngle = getLookAngle().normalize();
        Vec3 idealDirection = offset.normalize();

        double alignmentScore = lookAngle.dot(idealDirection);
        // clamp to MAX_EXPECTED_SPEED so the speed term stays in the same
        // magnitude range as the other reward components
        double speedTowardsTarget = Math.min(
                velocity.dot(idealDirection),
                MAX_EXPECTED_SPEED
        );

        double reward = 0.0;

        // speed reward, reward forward progress, penalize stopping only when
        // already well-aligned (alignmentScore > 0.5 ≈ within ±60° of target).
        // If the dragon is still turning, negative speed is expected and not punished.
        if (speedTowardsTarget > 0) {
            reward += speedTowardsTarget * SPEED_WEIGHT;
        } else if (alignmentScore > 0.5) {
            reward -= Math.abs(speedTowardsTarget) * SPEED_WEIGHT;
        }

        // alignment reward, only positive alignment is rewarded; negative alignment
        // is already implicitly punished by zero speed reward and distance growth
        reward += Math.max(0.0, alignmentScore) * ALIGNMENT_WEIGHT;

        // obstacle avoidance continuous penalty starting at 20 blocks (0.5 * 40)
        // giving the model enough reaction time at flight speed
        double obstacleDistanceRatio = getObstacleDistanceRatio();
        if (obstacleDistanceRatio < 0.5) {
            reward -= (0.5 - obstacleDistanceRatio) * OBSTACLE_PENALTY_WEIGHT;
        }

        // distance penalty normalized to [0, 1] against a 100-block reference so
        // it stays in scale with the other terms regardless of map size
        double normalizedDistance = Math.min(1.0, offset.length() / 100.0);
        reward -= normalizedDistance * 2.0;

        return reward;
    }

    public @NotNull Vec3 getLookAngle() {
        return this.dragon.getLookAngle();
    }

    /**
     * Performs a Paper API Block RayTrace to find the nearest obstacle in the looking direction.
     * @return A normalized ratio [0.0, 1.0] representing the distance to the collision.
     */
    private double getObstacleDistanceRatio() {
        World world = dragon.getBukkitEntity().getWorld();
        Vec3 pos  = dragon.position();
        Vec3 look = getLookAngle().normalize();

        Vector direction = new Vector(look.x, look.y, look.z);
        Location startLoc = new Location(world, pos.x, pos.y, pos.z);

        RayTraceResult result = world.rayTraceBlocks(startLoc, direction, MAX_RAYTRACE_DISTANCE);

        if (result != null && result.getHitBlock() != null) {
            double actualDistance = startLoc.distance(result.getHitBlock().getLocation());
            return Math.max(0.0, actualDistance / MAX_RAYTRACE_DISTANCE);
        }

        return 1.0;
    }

    public @NotNull List<SpartanSingleContextElement> buildContextElements() {
        var list = new ArrayList<SpartanSingleContextElement>();

        // global alignment, scalar dot product between current look and ideal direction
        // tells the model "how well" it is pointing, but not "where" to correct
        // yaw_error and pitch_error below carry the directional correction signal
        list.add(createContextElement("alignment", () -> {
            if (!dragon.hasTarget()) return 0.0;
            return getLookAngle().normalize().dot(dragon.getTargetOffset().normalize());
        }));

        // effective velocity, speed projected onto the ideal direction, normalized to [-1, 1]
        // positive = moving toward target, negative = drifting away
        list.add(createContextElement("effective_velocity", () -> {
            if (!dragon.hasTarget()) return 0.0;
            double effectiveSpeed = dragon.getDeltaMovement().dot(dragon.getTargetOffset().normalize());
            return Math.max(-1.0, Math.min(1.0, effectiveSpeed / MAX_EXPECTED_SPEED));
        }));

        // obstacle distance ratio, 1.0 = clear ahead, 0.0 = collision imminent
        list.add(createContextElement("obstacle_distance", this::getObstacleDistanceRatio));

        // target distance, normalized against a 100-block reference
        // gives the model a sense of proximity without unbounded values
        list.add(createContextElement("target_distance", () -> {
            if (!dragon.hasTarget()) return 1.0;
            return Math.min(1.0, dragon.getTargetOffset().length() / 100.0);
        }));

        // yaw error, horizontal angular error via cross product on the XZ plane
        // positive = target is to the right, negative = to the left
        // this is the directional signal the yawAction learns to correct
        list.add(createContextElement("yaw_error", () -> {
            if (!dragon.hasTarget()) return 0.0;
            Vec3 toTarget = dragon.getTargetOffset().normalize();
            Vec3 forward  = getLookAngle().normalize();
            double cross  = forward.x * toTarget.z - forward.z * toTarget.x;
            return Math.max(-1.0, Math.min(1.0, cross));
        }));

        // pitch error, vertical angular error via Y component difference
        // positive = target is above current look angle, negative = below
        // this is the directional signal the pitchAction learns to correct
        list.add(createContextElement("pitch_error", () -> {
            if (!dragon.hasTarget()) return 0.0;
            Vec3 toTarget    = dragon.getTargetOffset().normalize();
            Vec3 forward     = getLookAngle().normalize();
            double pitchDiff = toTarget.y - forward.y;
            return Math.max(-1.0, Math.min(1.0, pitchDiff));
        }));

        // current orientation, lets the recurrent network learn to anticipate
        // rotational inertia: a large yaw requires several ticks to complete
        list.add(createContextElement("current_yaw_normalized",   () -> dragon.getYRot() / 180.0));
        list.add(createContextElement("current_pitch_normalized", () -> dragon.getXRot() / 90.0));

        return list;
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    private SpartanSingleContextElement createContextElement(String id, DoubleSupplier valueSupplier) {
        return new SpartanSingleContextElement() {
            @Override
            public @Range(from = -1L, to = 1L) double getValue() {
                return valueSupplier.getAsDouble();
            }

            @Override
            public int getSize() { return 1; }

            @Override
            public void tick() {
            }

            @NotNull
            @Override
            public String getIdentifier() { return "dragon_ctx_" + id; }
        };
    }
}