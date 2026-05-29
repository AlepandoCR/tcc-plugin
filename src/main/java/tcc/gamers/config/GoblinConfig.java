package tcc.gamers.config;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Configurable(name = "GeneratedZombieConfig")
public class GoblinConfig implements TCCConfig {

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

    @ConfigValue(comment = "How often enemies looked up ")
    private final double targetLookupFrequency; // how often are enemies

    @ConfigValue(comment = "Distance in blocks to look for targets")
    private final double targetLookupDistance;

    @ConfigValue(comment = "Distance in blocks to look for friends")
    private final double friendLookupDistance;

    @ConfigValue(comment = "Entity key")
    private final String entityKey;

    @ConfigValue(comment = "Entity namespace")
    private final String entityNamespace;

    @ConfigValue(comment = "How much nearby friends contribute to focus recovery per tick")
    private final double focusFriendRecoveryRate;

    @ConfigValue(comment = "Max amount of worry for goblins")
    private final double maxWorry;

    @ConfigValue(comment = "Min amount of worry for goblins")
    private final double minWorry;

    @ConfigValue(comment = "Max amount of aggressiveness for goblins")
    private final double maxAggressiveness;

    @ConfigValue(comment = "Min amount of aggressiveness for goblins")
    private final double minAggressiveness;

    public GoblinConfig(
            float maxSpeed,
            float minSpeed,
            double modelScale,
            double friendLookupDistance,
            double targetLookupDistance,
            double targetLookupFrequency,
            double focusFriendRecoveryRate,
            double maxWorry,
            double minWorry,
            double maxAggressiveness,
            double minAggressiveness,
            @NotNull String modelName,
            @NotNull String nameMiniMessage,
            @NotNull String entityNamespace,
            @NotNull String entityKey
    ) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.modelName = modelName;
        this.modelScale = modelScale;
        this.nameMiniMessage = nameMiniMessage;
        this.targetLookupFrequency = targetLookupFrequency;
        this.targetLookupDistance = targetLookupDistance;
        this.entityKey = entityKey;
        this.entityNamespace = entityNamespace;
        this.focusFriendRecoveryRate = focusFriendRecoveryRate;
        this.maxWorry = maxWorry;
        this.minWorry = minWorry;
        this.maxAggressiveness = maxAggressiveness;
        this.minAggressiveness = minAggressiveness;
        this.friendLookupDistance = friendLookupDistance;
    }

    public GoblinConfig(@NotNull GeneratedZombieConfig config){
        this.maxSpeed = config.getMaxSpeed();
        this.minSpeed = config.getMinSpeed();
        this.modelName = config.getModelName();
        this.modelScale = config.getModelScale();
        this.nameMiniMessage = config.getNameMiniMessage();
        this.targetLookupDistance = config.getTargetLookupDistance();
        this.targetLookupFrequency = config.getTargetLookupFrequency();
        this.entityKey = config.getEntityKey();
        this.entityNamespace = config.getEntityNamespace();
        this.focusFriendRecoveryRate = config.getFocusFriendRecoveryRate();
        this.maxWorry = config.getMaxWorry();
        this.minWorry = config.getMinWorry();
        this.maxAggressiveness = config.getMaxAggressiveness();
        this.minAggressiveness = config.getMinAggressiveness();
        this.friendLookupDistance= config.getFriendLookupDistance();
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

   public @NotNull String getEntityNamespace() {
       return entityNamespace;
   }

   public @NotNull String getEntityKey() {
       return entityKey;
   }

    public double getTargetLookupFrequency() {
        return targetLookupFrequency;
    }

    public double getTargetLookupDistance() {
        return targetLookupDistance;
    }

    public double getFocusFriendRecoveryRate() {
        return focusFriendRecoveryRate;
    }

    public double getMaxWorry() {
        return maxWorry;
    }

    public double getMinWorry() {
        return minWorry;
    }

    public double getMaxAggressiveness() {
        return maxAggressiveness;
    }

    public double getMinAggressiveness() {
        return minAggressiveness;
    }

    public double getFriendLookupDistance() {
        return friendLookupDistance;
    }
}