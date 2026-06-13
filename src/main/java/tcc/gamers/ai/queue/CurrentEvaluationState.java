package tcc.gamers.ai.queue;

import org.jetbrains.annotations.NotNull;

/**
 * A mutable, volatile scratchpad that serves as the <em>live data bus</em> between
 * the {@link DragonQueueManager}'s processing loop and the Spartan context suppliers.
 */
public final class CurrentEvaluationState {

    public volatile double targetDirX = 0.0;
    public volatile double targetDirY = 0.0;
    public volatile double targetDirZ = 0.0;

    public volatile double velX = 0.0;
    public volatile double velY = 0.0;
    public volatile double velZ = 0.0;

    /** @see DragonStateRequest#obstacleDistanceRatio() */
    public volatile double obstacleDistanceRatio = 1.0; // default: clear path

    /** @see DragonStateRequest#targetDistance() */
    public volatile double targetDistance = 1.0; // default: far away

    /** @see DragonStateRequest#yawError() */
    public volatile double yawError = 0.0;

    /** @see DragonStateRequest#pitchError() */
    public volatile double pitchError = 0.0;

    public volatile double currentReward = 0.0;

    /**
     * Atomically (in the sense of sequential ordering on a single writer thread)
     * overwrites all fields with the values from the given request snapshot.
     *
     * @param request The immutable state snapshot to inject.
     */
    public void writeFrom(@NotNull DragonStateRequest request) {
        this.targetDirX = request.targetDirX();
        this.targetDirY = request.targetDirY();
        this.targetDirZ = request.targetDirZ();

        this.velX = request.velX();
        this.velY = request.velY();
        this.velZ = request.velZ();

        this.obstacleDistanceRatio = request.obstacleDistanceRatio();
        this.targetDistance = request.targetDistance();
        this.yawError = request.yawError();
        this.pitchError = request.pitchError();
    }

    /**
     * Resets all fields to their neutral/default values.
     * Called by the manager on shutdown or between processing epochs to prevent
     * stale data from a stopped dragon leaking into the next evaluation.
     */
    public void reset() {
        this.targetDirX = 0.0;
        this.targetDirY = 0.0;
        this.targetDirZ = 0.0;

        this.velX = 0.0;
        this.velY = 0.0;
        this.velZ = 0.0;

        this.obstacleDistanceRatio = 1.0;
        this.targetDistance = 1.0;
        this.yawError = 0.0;
        this.pitchError = 0.0;
        this.currentReward = 0.0;
    }
}