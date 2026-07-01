package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.util.MiniMessageUtil;

public abstract class RaceDto extends LocationDto{

    @ConfigValue(fieldName = "race-identifier", comment = "identifier for the race")
    private final String identifier;

    @ConfigValue(fieldName = "race-display-name", comment = "display name for the race")
    private final String displayName;

    @ConfigValue(fieldName = "x-radius", comment = "radios for the start location")
    private final double xRadius;

    @ConfigValue(fieldName = "y-radius", comment = "radios for the start location")
    private final double yRadius;

    @ConfigValue(fieldName = "z-radius", comment = "radios for the start location")
    private final double zRadius;

    @ConfigValue(fieldName = "tick-frequency", comment = "tick frequency (20.0 = 1sg) its a race so less than a second is ideal")
    private final double tickFrequency;

    @ConfigValue(fieldName = "checkpoint-detection-radius", comment = "radius for checkpoint detection")
    private final double checkpointDetectionRadius;


    public RaceDto(
            double xCoord,
            double yCoord,
            double zCoord,
            double xRadius,
            double yRadius,
            double zRadius,
            double tickFrequency,
            double checkpointDetectionRadius,
            @NotNull String worldName,
            @NotNull String identifier,
            @NotNull String displayName
    ) {
        super(
                xCoord,
                yCoord,
                zCoord,
                worldName
        );
        this.identifier = identifier;
        this.xRadius = xRadius;
        this.yRadius = yRadius;
        this.zRadius = zRadius;
        this.tickFrequency = tickFrequency;
        this.checkpointDetectionRadius = checkpointDetectionRadius;
        this.displayName = displayName;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public double getXRadius() {
        return xRadius;
    }

    public double getYRadius() {
        return yRadius;
    }

    public double getZRadius() {
        return zRadius;
    }

    public double getTickFrequency() {
        return tickFrequency;
    }

    public double getCheckpointDetectionRadius() {
        return checkpointDetectionRadius;
    }

    public @NotNull Component getDisplayName() {
        return MiniMessageUtil.INSTANCE.parseOrPlain(displayName);
    }

    public @NotNull String getDisplayNameString(){
        return displayName;
    }
}
