package tcc.gamers.raid;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.adapters.BukkitWorld;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.EmotionController;
import tcc.gamers.data.RaidMobDto;

import java.util.Optional;

public class RaidMob {

    private final @NotNull TCCPlugin plugin;

    private final @NotNull MythicMob mythicMob;

    private @Nullable ActiveMob spawnedMob;

    private final int level;

    private final @NotNull EmotionController emotionController;

    public RaidMob(
            @NotNull String mobId,
            @NotNull TCCPlugin plugin,
            int level
    ){
        this.plugin = plugin;
        this.mythicMob = buildMobOrThrow(mobId);
        this.level = level;
        this.emotionController = new EmotionController(
                this,
                plugin.getConfigManager().getRaidMobsConfig()
        );
    }

    public RaidMob(@NotNull RaidMobDto raidMobDto, @NotNull TCCPlugin plugin){
        this(
                raidMobDto.getMobId(),
                plugin,
                raidMobDto.getMobLevel()
        );
    }

    private @NotNull MythicMob buildMobOrThrow(@NotNull String mobId){
        var maybeMob = plugin.getMythicApi().getMobManager().getMythicMob(mobId);

        if(maybeMob.isEmpty()){
            throw new IllegalStateException("Mob id is not loaded in MythicMobs" + mobId);
        }

        return maybeMob.get();
    }

    public void spawnMob(@NotNull Location location){
        var mythicWorldWrapper = new BukkitWorld(location.getWorld());
        var abstractLocation = new AbstractLocation(
                mythicWorldWrapper,
                location.getX(),
                location.getY(),
                location.getZ()
        );
        this.spawnedMob = mythicMob.spawn(abstractLocation, level);
        var maybeEntity = getBukkitEntity();

    }

    public void despawnMob(){
        if(spawnedMob != null){
            spawnedMob.despawn();
            spawnedMob = null; // clean for next spawn
        }
    }

    public @NotNull MythicMob getMythicMob(){
        return this.mythicMob;
    }

    public @NotNull Optional<ActiveMob> getSpawnedEntity(){
        return Optional.ofNullable(spawnedMob);
    }

    public @NotNull Optional<Entity> getBukkitEntity(){
        if(spawnedMob == null){
            return Optional.empty();
        }
        return Optional.ofNullable( // nullable because we don't trust mythic mobs c:
                spawnedMob
                .getEntity()
                .getBukkitEntity()
        );
    }

    public boolean isSpawned(){
        return spawnedMob != null;
    }

    public boolean notSpawned(){ // method for lambda
        return !isSpawned();
    }

    public boolean isAlive(){
        if(!isSpawned()){
            return false;
        }

        if(spawnedMob != null){ // this is always true but compiler can't tell
            return !spawnedMob.isDead();
        }

        return false;
    }

    public @NotNull EmotionController getEmotionController(){
        return this.emotionController;
    }
}
