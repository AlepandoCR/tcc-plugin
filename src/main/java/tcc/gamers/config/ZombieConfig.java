package tcc.gamers.config;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Configurable(name = "GeneratedZombieConfig")
public class ZombieConfig implements TCCConfig {

    @ConfigValue(comment = "Maximum speed of the zombie in blocks per second.")
    private final double maxSpeed;

    @ConfigValue(comment = "Minimum speed of the zombie in blocks per second.")
    private final double minSpeed;

    @ConfigValue(comment = "Name of the zombie model to use.")
    private final String modelName;

    @ConfigValue(comment = "Scale factor for the zombie model.")
    private final double modelScale;

    @ConfigValue(comment = "MiniMessage string used as the zombie custom name.")
    private final String nameMiniMessage;

    public ZombieConfig(
            double maxSpeed,
            double minSpeed,
            @NotNull String modelName,
            double modelScale
    ) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.modelName = modelName;
        this.modelScale = modelScale;
        this.nameMiniMessage = "<red>Goblin";
    }

    public ZombieConfig(
            float maxSpeed,
            float minSpeed,
            @NotNull String modelName,
            double modelScale,
            @NotNull String nameMiniMessage
    ) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.modelName = modelName;
        this.modelScale = modelScale;
        this.nameMiniMessage = nameMiniMessage;
    }

    public ZombieConfig(@NotNull GeneratedZombieConfig config){
        this.maxSpeed = config.getMaxSpeed();
        this.minSpeed = config.getMinSpeed();
        this.modelName = config.getModelName();
        this.modelScale = config.getModelScale();
        this.nameMiniMessage = config.getNameMiniMessage();
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    @NotNull
    public String getModelName() {
        return modelName;
    }

    public double getModelScale() {
        return modelScale;
    }

    @NotNull
    public String getNameMiniMessage() {
        return nameMiniMessage;
    }
}