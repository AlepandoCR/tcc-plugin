package tcc.gamers.config;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Configurable(name = "GeneratedHorseConfig")
public class HorseConfig implements PluginConfig {

    @ConfigValue(comment = "Maximum speed of the horse in blocks per second.")
    private final double maxSpeed;

    @ConfigValue(comment = "Minimum speed of the horse in blocks per second.")
    private final double minSpeed;

    @ConfigValue(comment = "Name of the horse model to use.")
    private final String modelName;

    @ConfigValue(comment = "Scale factor for the horse model.")
    private final double modelScale;

    @ConfigValue(comment = "MiniMessage string used as the horse custom name. Use %player% to include the player name.")
    private final String nameMiniMessage;

    public HorseConfig(
            double maxSpeed,
            double minSpeed,
            @NotNull String modelName,
            double modelScale
    ) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.modelName = modelName;
        this.modelScale = modelScale;
        this.nameMiniMessage = "<green>Taxi for %player%";
    }

    public HorseConfig(
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

    public HorseConfig(@NotNull GeneratedHorseConfig config){
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
