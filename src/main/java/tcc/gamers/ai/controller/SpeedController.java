package tcc.gamers.ai.controller;

import org.bukkit.Bukkit;
import tcc.gamers.TCCPlugin;

public class SpeedController implements Controller {

    // current (smoothed) speed exposed to navigation
    private float currentSpeed;
    // target speed requested by the action/model
    private float targetSpeed;

    private final float maxSpeed;
    private final float minSpeed;
    // maximum change in speed per tick (units per tick) - controls smoothness / acceleration
    private final float maxDeltaPerTick;

    public SpeedController(
            float initialSpeed,
            float maxSpeed,
            float minSpeed
    ) {
        this(initialSpeed, maxSpeed, minSpeed, 0.1f);
    }

    /**
     * New constructor allowing configuration of acceleration (max delta per tick).
     * @param initialSpeed initial speed applied to both current and target
     * @param maxSpeed maximum allowed speed
     * @param minSpeed minimum allowed speed
     * @param maxDeltaPerTick maximum change in speed per tick (smaller = smoother)
     */
    public SpeedController(
            float initialSpeed,
            float maxSpeed,
            float minSpeed,
            float maxDeltaPerTick
    ) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.maxDeltaPerTick = Math.max(0f, maxDeltaPerTick);
        float clamped = clamp(initialSpeed);
        this.currentSpeed = clamped;
        this.targetSpeed = clamped;
    }

    /**
     * Return the current smoothed speed used by navigation.
     */
    public float getSpeed() {
        return currentSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getMinSpeed() {
        return minSpeed;
    }

    /** Immediately set both current and target speed (no smoothing).
     * Kept for compatibility.
     */
    public void setSpeed(float speed) {
        float clamped = clamp(speed);
        this.currentSpeed = clamped;
        this.targetSpeed = clamped;
        // Debug log to trace speed changes
        try {
            Bukkit.getLogger().info("[tcc-plugin][SpeedController] setSpeed requested=" + speed + " clamped=" + clamped);
        } catch (NoClassDefFoundError ignored) {
            // In case this is used in a non-Bukkit context during tests/builds, ignore
        }
    }

    /**
     * Request a target speed. The controller will smooth the actual current speed towards this
     * value across subsequent ticks (at most {@code maxDeltaPerTick} per tick).
     */
    public void setTargetSpeed(float speed) {
        this.targetSpeed = clamp(speed);
    }

    /**
     * Advance the current speed towards the target speed. Call once per server tick.
     */
    @Override
    public void tick() {
        float diff = targetSpeed - currentSpeed;
        if (Math.abs(diff) <= 1e-6f) return;
        float change = Math.signum(diff) * Math.min(Math.abs(diff), maxDeltaPerTick);
        currentSpeed += change;
        // Optionally log fine-grained updates at FINE level
        try {
            TCCPlugin.getInstance().getLogger().fine("[tcc-plugin][SpeedController] tick update current=" + currentSpeed + " target=" + targetSpeed + " change=" + change);
        } catch (NoClassDefFoundError ignored) {
        }
    }

    private float clamp(float value) {
        return Math.max(minSpeed, Math.min(maxSpeed, value));
    }
}
