package tcc.gamers.ai.spartan.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spartan.api.engine.action.type.SpartanAction;
import tcc.gamers.ai.controller.SpeedController;
import tcc.gamers.ai.spartan.context.PointDistanceContextElement;
import tcc.gamers.ai.spartan.context.PointInclinationContextElement;
import tcc.gamers.ai.spartan.monitor.SpartanActionMonitor;

/**
 * Spartan action responsible for regulating the entity's speed.
 * <p>
 * This action evaluates both the distance to the target and the terrain's inclination
 * to compute optimal velocity, applying penalties for abrupt acceleration changes (jerk)
 * to ensure smooth policy convergence.
 */
public class ChangeSpeedAction implements SpartanAction {

    private final PointDistanceContextElement distanceContext;
    private final PointInclinationContextElement inclinationContext;
    private final SpeedController speedController;
    private final SpartanActionMonitor actionMonitor;

    private double previousRequestedMagnitude;
    private double lastRequestedMagnitude;

    /**
     * Constructs a new ChangeSpeedAction.
     *
     * @param speedController    The controller managing entity movement speed.
     * @param distanceContext    Context element providing the distance to the target.
     * @param inclinationContext Context element providing the pitch to the target.
     * @param actionMonitor      Optional monitor for telemetry and debugging.
     */
    public ChangeSpeedAction(
            @NotNull SpeedController speedController,
            @NotNull PointDistanceContextElement distanceContext,
            @NotNull PointInclinationContextElement inclinationContext,
            @Nullable SpartanActionMonitor actionMonitor
    ) {
        this.speedController = speedController;
        this.distanceContext = distanceContext;
        this.inclinationContext = inclinationContext;
        this.actionMonitor = actionMonitor;

        double init = speedController.getSpeed();
        this.previousRequestedMagnitude = init;
        this.lastRequestedMagnitude = init;
    }

    @Override
    public @NotNull String identifier() {
        return "change_speed";
    }

    @Override
    public double taskMaxMagnitude() {
        return speedController.getMaxSpeed();
    }

    @Override
    public double taskMinMagnitude() {
        return speedController.getMinSpeed();
    }

    @Override
    public void task(double normalizedMagnitude) {
        if (actionMonitor != null) {
            actionMonitor.recordTask((float) normalizedMagnitude);
        }

        this.previousRequestedMagnitude = this.lastRequestedMagnitude;
        this.lastRequestedMagnitude = normalizedMagnitude;

        speedController.setTargetSpeed((float) normalizedMagnitude);
    }

    @Override
    public double award() {
        if (actionMonitor != null) {
            actionMonitor.recordAwardCheck();
        }

        double distance = distanceContext.getDistance();
        double inclination = inclinationContext.getValue();

        double desiredSpeed = desiredSpeedForState(distance, inclination);
        double currentSpeed = speedController.getSpeed();
        double finalReward = getFinalReward(currentSpeed, desiredSpeed);
        return Math.max(-1.0, Math.min(1.0, finalReward));
    }

    private double getFinalReward(double currentSpeed, double desiredSpeed) {
        double range = Math.max(0.0001, speedController.getMaxSpeed() - speedController.getMinSpeed());

        double error = Math.abs(currentSpeed - desiredSpeed) / range;
        double baseReward = Math.max(-1.0, 1.0 - (error * 2.0));

        double requestedDelta = Math.abs(lastRequestedMagnitude - previousRequestedMagnitude);
        double normalizedJerk = requestedDelta / Math.max(1e-6, range);
        double smoothingWeight = 0.5;

        return baseReward - (smoothingWeight * normalizedJerk);
    }

    /**
     * Calculates the optimal speed based on spatial distance and terrain slope.
     *
     * @param distance    The physical distance to the target coordinate.
     * @param inclination The normalized pitch to the target [-1.0, 1.0].
     * @return The ideal speed magnitude for the current state.
     */
    private double desiredSpeedForState(double distance, double inclination) {
        double minSpeed = speedController.getMinSpeed();
        double maxSpeed = speedController.getMaxSpeed();
        double nearDistance = 5.0;
        double farDistance = 15.0;

        double baseDesired;
        if (distance <= nearDistance) {
            baseDesired = minSpeed;
        } else if (distance >= farDistance) {
            baseDesired = maxSpeed;
        } else {
            double normalized = (distance - nearDistance) / (farDistance - nearDistance);
            baseDesired = minSpeed + (normalized * (maxSpeed - minSpeed));
        }

        if (inclination < -0.2) {
            double safeDownhillSpeed = minSpeed + ((maxSpeed - minSpeed) * 0.2);
            return Math.min(baseDesired, safeDownhillSpeed);
        } else if (inclination > 0.3) {
            double uphillMomentumSpeed = minSpeed + ((maxSpeed - minSpeed) * 0.8);
            return Math.max(baseDesired, uphillMomentumSpeed);
        }

        return baseDesired;
    }
}