package tcc.gamers.ai.controller;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.config.GoblinConfig;

public class EmotionController implements Controller{

    private double worry = 0.0; // increases when having a great target density, having a low goblinDensity or being low in hp

    private double aggressiveness = 0.0; // increases when having a low target density, having a great goblinDensity or being high in hp

    private double focus = 0.0; // focus lowers when hp is lowered, comes up by having a great goblinDensity or healing

    private double targetDensity = 0.0; // numerical representation of "how many targets are nearby"

    private double friendDensity = 0.0; // numerical representation of "how many friends are nearby"

    private double previousHpRatio = 0.0;

    private final @NotNull LivingEntity entity;

    private final @NotNull GoblinConfig goblinConfig;

    private @NotNull EmotionStrategy emotionStrategy;

    public EmotionController(
            @NotNull LivingEntity entity,
            @NotNull GoblinConfig config
    ) {
        this.entity = entity;
        this.goblinConfig = config;
        this.emotionStrategy = EmotionStrategy.AGGRESSIVE_REPOSITION;
    }

    @Override
    public void tick() {
        var healthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            var totalHealth = healthAttribute.getValue();

            var currentHealth = entity.getHealth();

            double hpRatio = currentHealth / totalHealth;
            double hpDelta = hpRatio - previousHpRatio;

            double focusFactor = hpDelta + (friendDensity * goblinConfig.getFocusFriendRecoveryRate());

            this.focus = Math.clamp(this.focus + focusFactor, 0.0, 1.0);
            this.previousHpRatio = hpRatio;
        }

        updateEmotionStrategy();
    }

    private void updateEmotionStrategy() {
        boolean highWorry = worry > goblinConfig.getMaxWorry();
        boolean highAggression = aggressiveness > goblinConfig.getMaxAggressiveness();
        boolean highFocus = focus > 0.5;

        if (highWorry && !highAggression) {
            this.emotionStrategy = EmotionStrategy.COVER_REPOSITION;
            return;
        }

        if (highFocus) {
            this.emotionStrategy = highAggression
                    ? EmotionStrategy.PUSH_REPOSITION
                    : EmotionStrategy.SUPPORT_REPOSITION;
            return;
        }

        this.emotionStrategy = highAggression
                ? EmotionStrategy.AGGRESSIVE_REPOSITION
                : EmotionStrategy.DEFENSIVE_REPOSITION;
    }

    public double getWorry() {
        return worry;
    }

    public void setWorry(double worry) {
        this.worry = worry;
    }

    public double getAggressiveness() {
        return aggressiveness;
    }


    public void setAggressiveness(double aggressiveness) {
        this.aggressiveness = aggressiveness;
    }

    public double getFocus() {
        return focus;
    }

    public void setFocus(double focus) {
        this.focus = focus;
    }

    public void increaseFocus(){
        focus++;
    }

    public void decreaseFocus(){
        focus--;
    }

    public double getTargetDensity() {
        return targetDensity;
    }

    public void setTargetDensity(double targetDensity) {
        this.targetDensity = targetDensity;
    }

    public void updateTargetDensity(int entityAmount) {
        double radius = goblinConfig.getTargetLookupDistance();
        this.targetDensity = 1.0 - Math.exp(-entityAmount / radius);
    }

    public double getFriendDensity() {
        return friendDensity;
    }

    public void setFriendDensity(double friendDensity) {
        this.friendDensity = friendDensity;
    }

    public void updateFriendDensity(int entityAmount) {
        double radius = goblinConfig.getTargetLookupDistance();
        this.friendDensity = 1.0 - Math.exp(-entityAmount / radius);
    }

    private enum EmotionStrategy{
        DEFENSIVE_REPOSITION, // low focus - high worry - low aggressiveness (move to the closest ally)
        AGGRESSIVE_REPOSITION, // low focus - low worry - high aggressiveness (move to the closes ally with the highest target density)
        SUPPORT_REPOSITION, // high focus - low worry - low aggressiveness (move to the closest ally with the highest target density and the most worry) not the same as aggressive
        PUSH_REPOSITION, // high focus - low worry - high aggressiveness (move to the closest ally with the highest target density and lowest worry)
        COVER_REPOSITION, // whatever focus - high worry - low aggressiveness (move to the closest ally with the most worry and lowest goblin density)
    }
}


