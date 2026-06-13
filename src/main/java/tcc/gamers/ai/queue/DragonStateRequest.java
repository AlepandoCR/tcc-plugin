package tcc.gamers.ai.queue;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.ai.controller.DragonLookController;

/**
 * An immutable, point-in-time snapshot of a single dragon's sensory state,
 * captured at the moment it was submitted to the {@link DragonQueueManager}.
 */
public record DragonStateRequest(
        double targetDirX,
        double targetDirY,
        double targetDirZ,
        double velX,
        double velY,
        double velZ,
        double obstacleDistanceRatio,
        double targetDistance,
        double yawError,
        double pitchError,

        @NotNull DragonLookController requester

) {

    public DragonStateRequest {
        validateFinite("targetDirX",             targetDirX);
        validateFinite("targetDirY",             targetDirY);
        validateFinite("targetDirZ",             targetDirZ);
        validateFinite("velX",                   velX);
        validateFinite("velY",                   velY);
        validateFinite("velZ",                   velZ);
        validateFinite("obstacleDistanceRatio",  obstacleDistanceRatio);
        validateFinite("targetDistance",         targetDistance);
        validateFinite("yawError",               yawError);
        validateFinite("pitchError",             pitchError);
    }


    @NotNull
    public static DragonStateRequest of(
            double targetDirX,
            double targetDirY,
            double targetDirZ,
            double velX,
            double velY,
            double velZ,
            double obstacleDistanceRatio,
            double targetDistance,
            double yawError,
            double pitchError,
            @NotNull DragonLookController requester
    ) {
        return new DragonStateRequest(
                targetDirX,
                targetDirY,
                targetDirZ,
                velX,
                velY,
                velZ,
                obstacleDistanceRatio,
                targetDistance,
                yawError,
                pitchError,
                requester
        );
    }

    private static void validateFinite(@NotNull String fieldName, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "DragonStateRequest field '" + fieldName + "' must be finite, got: " + value
            );
        }
    }
}