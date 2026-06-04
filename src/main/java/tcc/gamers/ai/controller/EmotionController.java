package tcc.gamers.ai.controller;

import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.config.RaidMobsConfig;
import tcc.gamers.raid.RaidMob;

public class EmotionController implements Controller{

    private double worry = 0.0; // increases when having a great target density, having a low goblinDensity or being low in hp

    private double aggressiveness = 0.0; // increases when having a low target density, having a great goblinDensity or being high in hp

    private double focus = 0.0; // focus lowers when hp is lowered, comes up by having a great goblinDensity or healing

    private double targetDensity = 0.0; // numerical representation of "how many targets are nearby"

    private double friendDensity = 0.0; // numerical representation of "how many friends are nearby"

    private double previousHpRatio = 0.0;

    private final @NotNull RaidMob entity;

    private final @NotNull RaidMobsConfig raidMobsConfig;

    private @NotNull EmotionStrategy emotionStrategy;

    public EmotionController(
            @NotNull RaidMob entity,
            @NotNull RaidMobsConfig config
    ) {
        this.entity = entity;
        this.raidMobsConfig = config;
        this.emotionStrategy = EmotionStrategy.AGGRESSIVE_REPOSITION;
    }

    @Override
    public void tick() {
        if(entity.isSpawned() && entity.isAlive()){
            var maybeSpawnedEntity = entity.getSpawnedEntity();

            maybeSpawnedEntity.ifPresent(this::doTick);
            updateEmotionStrategy();
        }
    }

    private void doTick(@NotNull ActiveMob spawnedEntity) {
        var unnecessaryMythicMobsEntityWrapper = spawnedEntity.getEntity(); // just to make fun of them c:

        var maxHealth = spawnedEntity.getEntity().getMaxHealth();

        var currentHealth = unnecessaryMythicMobsEntityWrapper.getHealth();

        double hpRatio = currentHealth / maxHealth;
        double hpDelta = hpRatio - previousHpRatio;

        double focusFactor = hpDelta + (friendDensity * raidMobsConfig.getFocusFriendRecoveryRate());

        this.focus = Math.clamp(this.focus + focusFactor, 0.0, 1.0);
        this.previousHpRatio = hpRatio;
    }

    private void updateEmotionStrategy() {
        boolean highWorry = worry > raidMobsConfig.getMaxWorry();
        boolean highAggression = aggressiveness > raidMobsConfig.getMaxAggressiveness();
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
        double radius = raidMobsConfig.getTargetLookupDistance();
        this.targetDensity = 1.0 - Math.exp(-entityAmount / radius);
    }

    public double getFriendDensity() {
        return friendDensity;
    }

    public void setFriendDensity(double friendDensity) {
        this.friendDensity = friendDensity;
    }

    public void updateFriendDensity(int entityAmount) {
        double radius = raidMobsConfig.getTargetLookupDistance();
        this.friendDensity = 1.0 - Math.exp(-entityAmount / radius);
    }

    public @NotNull EmotionStrategy getEmotionStrategy(){
        return emotionStrategy;
    }


}


