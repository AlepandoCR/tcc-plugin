package tcc.gamers.ball.physics;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ball.BallController;

import java.util.UUID;

public class PhysicManager {

    private final BallController controller;

    private final @NotNull TCCPlugin plugin;

    @NotNull
    private Vector previousDeltaMovement = new Vector(0, 0, 0);

    // Physics Tuning Constants
    /** Horizontal bounce velocity retention (walls) */
    public static final double HORIZONTAL_BOUNCE_FACTOR = 0.4;
    /** Vertical bounce velocity retention (floor/ceiling) */
    public static final double VERTICAL_BOUNCE_FACTOR = 0.3;
    /** Minimum bounce speed to prevent stuck balls */
    public static final double MIN_BOUNCE_SPEED = -0.1;
    /** Ground friction XZ (horizontal damping) */
    public static final double GROUND_DRAG_XZ = 0.9;
    /** Ground friction Y (vertical damping) */
    public static final double GROUND_DRAG_Y = 0.98;
    /** Dribbling pull factor (player interaction) */
    public static final double DRIBBLING_PULL_FACTOR = 0.5;

    /**
     * Constructs physics manager with ball controller reference.
     * @param controller ball state manager
     */
    public PhysicManager(@NotNull BallController controller, @NotNull TCCPlugin plugin) {
        this.controller = controller;
        this.plugin = plugin;
    }

    /**
     * Main physics calculation loop for ball entity.
     * Applies bounces, friction, and player interactions every server tick.
     * Call from ball entity's aiStep() or custom tick method.
     *
     * @param ball NMS ball entity with velocity data
     */
    public void calculateBallPhysics(@NotNull LivingEntity ball) {
        org.bukkit.entity.LivingEntity bukkitBall = controller.getBukkitBall();

        boolean wasVerticalCollision = ball.verticalCollision;
        boolean wasHorizontalCollision = ball.horizontalCollision;

        // Store current velocity before it's modified by collisions or gravity
        saveOriginalSpeed(wasVerticalCollision, wasHorizontalCollision, ball);

        // Scan and process interactions with nearby players/entities
        handleEntityInteraction(ball, bukkitBall);

        // Apply bounce logic based on the stored previous movement
        horizontalBounce(ball);
        verticalBounce(ball, wasVerticalCollision);

        // Apply ground friction
        groundDrag(ball);
    }

    private void saveOriginalSpeed(
            boolean wasVerticalCollision,
            boolean wasHorizontalCollision,
            @NotNull LivingEntity ball
    ) {
        if (!wasVerticalCollision && !wasHorizontalCollision) {
            Vec3 delta = ball.getDeltaMovement();
            previousDeltaMovement = new Vector(delta.x, delta.y, delta.z);
        }
    }

    private void handleEntityInteraction(
            @NotNull LivingEntity ball,
            @NotNull org.bukkit.entity.LivingEntity bukkitBall
    ) {
        org.bukkit.entity.LivingEntity nearestEntity = null;
        double nearestDistance = Double.MAX_VALUE;

        // Scan nearby entities in a 1.0 block radius
        for (Entity entity : bukkitBall.getNearbyEntities(1.0, 1.0, 1.0)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !entity.equals(bukkitBall)) {
                double distance = entity.getLocation().distance(bukkitBall.getLocation());
                if (distance <= 1.0 && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEntity = (org.bukkit.entity.LivingEntity) entity;
                }
            }
        }

        if (nearestEntity != null) {
            processDribble(nearestEntity, bukkitBall, ball);
        }
    }

    private void processDribble(
            @NotNull org.bukkit.entity.LivingEntity dribbler,
            @NotNull org.bukkit.entity.LivingEntity bukkitBall,
            @NotNull LivingEntity ball
    ) {
        // Sync the last interacted UUID with the central BallController
        controller.setLastInteractedEntity(dribbler.getUniqueId());

        dribble(bukkitBall, dribbler, ball);
    }

    private void dribble(
            @NotNull org.bukkit.entity.LivingEntity bukkitBall,
            @NotNull org.bukkit.entity.LivingEntity dribbler,
            @NotNull LivingEntity ball
    ) {
        Vector direction = bukkitBall.getLocation().toVector()
                .subtract(dribbler.getLocation().toVector())
                .normalize();

        Vec3 currentDelta = ball.getDeltaMovement();

        // Push the ball away from the dribbler horizontally
        Vec3 newMovement = new Vec3(direction.getX() * DRIBBLING_PULL_FACTOR, currentDelta.y, direction.getZ() * DRIBBLING_PULL_FACTOR);
        ball.setDeltaMovement(newMovement);
        ball.move(MoverType.SELF, newMovement);
    }

    private void groundDrag(@NotNull LivingEntity ball) {
        if (ball.onGround()) {
            Vec3 current = ball.getDeltaMovement();
            // Reduce velocity to simulate friction (X, Y, Z)
            Vec3 reducedMovement = current.multiply(GROUND_DRAG_XZ, GROUND_DRAG_Y, GROUND_DRAG_XZ);
            ball.setDeltaMovement(reducedMovement);
        }
    }

    private void verticalBounce(@NotNull LivingEntity ball, boolean wasVerticalCollision) {
        if (ball.verticalCollision) {
            double verticalSpeed = -previousDeltaMovement.getY() * VERTICAL_BOUNCE_FACTOR;

            // Ensure a minimum bounce threshold
            if (Math.abs(verticalSpeed) < Math.abs(MIN_BOUNCE_SPEED)) {
                verticalSpeed = MIN_BOUNCE_SPEED;
            }

            Vec3 currentDelta = ball.getDeltaMovement();
            Vec3 newMovement = new Vec3(currentDelta.x, verticalSpeed, currentDelta.z);
            ball.setDeltaMovement(newMovement);
            ball.move(MoverType.SELF, newMovement);
        } else if (!wasVerticalCollision) {
            Vec3 currentDelta = ball.getDeltaMovement();
            previousDeltaMovement = new Vector(currentDelta.x, currentDelta.y, currentDelta.z);
        }
    }

    private void horizontalBounce(@NotNull LivingEntity ball) {
        if (ball.horizontalCollision) {
            Vec3 currentDelta = ball.getDeltaMovement();
            double xSpeed = previousDeltaMovement.getX() * HORIZONTAL_BOUNCE_FACTOR;
            double zSpeed = previousDeltaMovement.getZ() * HORIZONTAL_BOUNCE_FACTOR;

            // When Minecraft processes a collision, it zeros out the velocity on the blocked axis.
            // We use this to detect which face (X or Z) we hit.
            if (Math.abs(currentDelta.x) < 0.05 && Math.abs(previousDeltaMovement.getX()) > 0.05) {
                // Hit a block on the X axis, invert X
                xSpeed = -previousDeltaMovement.getX() * HORIZONTAL_BOUNCE_FACTOR;
            }

            if (Math.abs(currentDelta.z) < 0.05 && Math.abs(previousDeltaMovement.getZ()) > 0.05) {
                // Hit a block on the Z axis, invert Z
                zSpeed = -previousDeltaMovement.getZ() * HORIZONTAL_BOUNCE_FACTOR;
            }

            // Invert direction smoothly using the minimum bounce thresholds
            if (Math.abs(xSpeed) < Math.abs(MIN_BOUNCE_SPEED) && Math.abs(previousDeltaMovement.getX()) >= 0.05) {
                xSpeed = Math.signum(xSpeed) * Math.abs(MIN_BOUNCE_SPEED);
            }
            if (Math.abs(zSpeed) < Math.abs(MIN_BOUNCE_SPEED) && Math.abs(previousDeltaMovement.getZ()) >= 0.05) {
                zSpeed = Math.signum(zSpeed) * Math.abs(MIN_BOUNCE_SPEED);
            }

            Vec3 newMovement = new Vec3(xSpeed, currentDelta.y, zSpeed);

            ball.setDeltaMovement(newMovement);
            ball.move(MoverType.SELF, newMovement);
        }
    }


    /**
     * Executes the kick for the given entity with the provided charge power (in hypothetical ms, e.g. 0-3000).
     */
    public void kickBall(@NotNull org.bukkit.entity.LivingEntity kicker, double charge) {
        UUID kickerId = kicker.getUniqueId();
        org.bukkit.entity.LivingEntity ballEntity = controller.getBukkitBall();

        // Dynamic range detection based on kicker speed
        double range = (kicker.getVelocity().length() > 0.21) ? 3.0 : 2.5;

        // Verify if the ball is still within reach
        if (ballEntity.getLocation().distance(kicker.getLocation()) > range) {
            return;
        }

        if (charge > 0) {
            // Update the last interacted entity in the central controller
            controller.setLastInteractedEntity(kickerId);

            ballEntity.getScheduler().run(plugin, (_ -> {
                        Vector kickDirection = this.calculateKickVector(kicker, charge);

                        ballEntity.setVelocity(kickDirection);

                    }),
                    () -> {}
            );


            // Audio feedback for nearby players
            for (Player player : kicker.getLocation().getNearbyPlayers(6.0)) {
                player.playSound(kicker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.2f, 0f);
            }
        }
    }

    /**
     * Calculates the vector for a kick given the entity and charge duration.
     */
    public Vector calculateKickVector(@NotNull Entity kicker, double charge) {
        double normalizedCharge = charge / 3000.0;
        double horizontalForce = normalizedCharge * 6.0;
        double verticalForce = normalizedCharge * 1.5;

        Vector kickDirection = kicker.getLocation().getDirection().multiply(horizontalForce);
        kickDirection.setY(kickDirection.getY() + verticalForce);
        return kickDirection;
    }
}