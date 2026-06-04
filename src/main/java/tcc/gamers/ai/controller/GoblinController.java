package tcc.gamers.ai.controller;

import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.config.RaidMobsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GoblinController<EntityType extends Entity> implements Controller{

    private final @NotNull TCCPlugin plugin;

    private final @NotNull EntityType entity;

    private @NotNull Location currentTarget;

    private final @NotNull List<LivingEntity> nearbyEnemies;

    private final @NotNull List<LivingEntity> nearbyFriends;

    private final @NotNull RaidMobsConfig raidMobsConfig;

    private final @NotNull EmotionController emotionController;

    private final @NotNull NamespacedKey friendNamespacedKey;

    private final @NotNull NamespacedKey targetDensityNamespacedKey;

    private final @NotNull NamespacedKey friendDensityNamespacedKey;

    private final @NotNull NamespacedKey worryNamespacedKey;

    private final @NotNull NamespacedKey aggressivenessNamespacedKey;

    private long ticks = 0L;


    public GoblinController(
            @NotNull TCCPlugin plugin,
            @NotNull EntityType entity,
            @NotNull RaidMobsConfig raidMobsConfig,
            @NotNull EmotionController emotionController
    ) {
        this.plugin = plugin;
        this.entity = entity;
        this.currentTarget = entity.getBukkitEntity().getLocation();
        this.raidMobsConfig = raidMobsConfig;
        this.emotionController = emotionController;
        this.nearbyEnemies = new ArrayList<>();
        this.nearbyFriends = new ArrayList<>();
        this.friendNamespacedKey = new NamespacedKey(raidMobsConfig.getEntityNamespace(), raidMobsConfig.getEntityKey());
        this.targetDensityNamespacedKey = new NamespacedKey(raidMobsConfig.getEntityNamespace(), "target_density");
        this.friendDensityNamespacedKey = new NamespacedKey(raidMobsConfig.getEntityNamespace(), "friend_density");
        this.worryNamespacedKey = new NamespacedKey(raidMobsConfig.getEntityNamespace(), "worry");
        this.aggressivenessNamespacedKey = new NamespacedKey(raidMobsConfig.getEntityNamespace(), "aggressiveness");
    }

    @Override
    public void tick() {
        // update danger
        if(ticks % raidMobsConfig.getTargetLookupFrequency() == 0) {
            nearbyEnemies.clear();
            nearbyFriends.clear();
            // update nearbyEnemies and targetDensity
            var configDistance = raidMobsConfig.getTargetLookupDistance();

            var teamConfigDistance = raidMobsConfig.getFriendLookupDistance();

            var closeEntities = this.entity.getBukkitEntity()
                    .getNearbyEntities(
                            configDistance,
                            configDistance,
                            configDistance
                    )
                    .stream()
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .toList();

            var closePlayers = closeEntities
                    .stream()
                    .filter(Player.class::isInstance)
                    .map(Player.class::cast).toList();

            nearbyEnemies.addAll(closePlayers);

            var teamEntityLookup = this.entity.getBukkitEntity() // look for entities in other range
                    .getNearbyEntities(
                            teamConfigDistance,
                            teamConfigDistance,
                            teamConfigDistance
                    )
                    .stream()
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .toList();

            var closeFriends = teamEntityLookup
                    .stream()
                    .filter(livingEntity -> livingEntity.getPersistentDataContainer().has(friendNamespacedKey))
                    .toList();

            nearbyFriends.addAll(closeFriends);
        }

        emotionController.updateTargetDensity(nearbyEnemies.size());

        emotionController.updateFriendDensity(nearbyFriends.size());

        writeOnEntity();

        ticks++;
    }

    private void writeOnEntity() {
        var bukkitEntity = this.entity.getBukkitEntity();
        var dataContainer = bukkitEntity.getPersistentDataContainer();
        dataContainer.set(
                worryNamespacedKey,
                PersistentDataType.DOUBLE,
                emotionController.getWorry()
        );

        dataContainer.set(
                aggressivenessNamespacedKey,
                PersistentDataType.DOUBLE,
                emotionController.getAggressiveness()
        );

        dataContainer.set(
                targetDensityNamespacedKey,
                PersistentDataType.DOUBLE,
                emotionController.getTargetDensity()
        );

        dataContainer.set(
                friendDensityNamespacedKey,
                PersistentDataType.DOUBLE,
                emotionController.getFriendDensity()
        );
    }

    private double readDoubleOnEntity(@NotNull NamespacedKey key) {
        var bukkitEntity = this.entity.getBukkitEntity();
        var dataContainer = bukkitEntity.getPersistentDataContainer();
        return dataContainer.getOrDefault(
                key,
                PersistentDataType.DOUBLE,
                0.0
        );
    }

    private double readDoubleOnFriend(@NotNull NamespacedKey key, @NotNull org.bukkit.entity.Entity entity) {
        var dataContainer = entity.getPersistentDataContainer();
        return dataContainer.getOrDefault(
                key,
                PersistentDataType.DOUBLE,
                0.0
        );
    }

    private @NotNull Optional<LivingEntity> getNearestFriend() {
        if(nearbyFriends.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sortByMinDistance(new ArrayList<>(nearbyFriends)).getFirst());
    }

    private @NotNull Optional<LivingEntity> getFriendWithHighestTargetDensity() {
        if(nearbyFriends.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(sortByMaxTargetDensity(new ArrayList<>(nearbyFriends)).getFirst());
    }

    private @NotNull Optional<LivingEntity> getFriendWithHighestTargetDensityAndMaxWorry() {
        if (nearbyFriends.isEmpty()) return Optional.empty();

        return nearbyFriends.stream()
                .max(Comparator.comparingDouble(
                                (LivingEntity e) -> readDoubleOnFriend(targetDensityNamespacedKey, e))
                        .thenComparingDouble(
                                e -> readDoubleOnFriend(worryNamespacedKey, e))
                );
    }

    private @NotNull Optional<LivingEntity> getFriendWithHighestTargetDensityAndMinWorry() {
        if (nearbyFriends.isEmpty()) return Optional.empty();

        return nearbyFriends.stream()
                .max(Comparator.comparingDouble(
                                (LivingEntity e) -> readDoubleOnFriend(targetDensityNamespacedKey, e))
                        .thenComparingDouble(
                                e -> -readDoubleOnFriend(worryNamespacedKey, e))
                );
    }

    private @NotNull Optional<LivingEntity> getFriendWithLowestFriendDensityAndMaxWorry() {
        if (nearbyFriends.isEmpty()) return Optional.empty();

        return nearbyFriends.stream()
                .max(Comparator.comparingDouble(
                                (LivingEntity e) -> -readDoubleOnFriend(friendDensityNamespacedKey, e))
                        .thenComparingDouble(
                                e -> readDoubleOnFriend(worryNamespacedKey, e))
                );
    }

    private @NotNull List<LivingEntity> sortByMaxTargetDensity(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(targetDensityNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(targetDensityNamespacedKey, entity2);
            return Double.compare(density2, density1);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMinDistance(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var distance1 = entity1.getLocation().distanceSquared(entity.getBukkitEntity().getLocation());
            var distance2 = entity2.getLocation().distanceSquared(entity.getBukkitEntity().getLocation());
            return Double.compare(distance1, distance2);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMaxFriendDensity(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(friendDensityNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(friendDensityNamespacedKey, entity2);
            return Double.compare(density2, density1);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMinFriendDensity(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(friendDensityNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(friendDensityNamespacedKey, entity2);
            return Double.compare(density1, density2);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMaxAggressiveness(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(aggressivenessNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(aggressivenessNamespacedKey, entity2);
            return Double.compare(density2, density1);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMaxWorry(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(worryNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(worryNamespacedKey, entity2);
            return Double.compare(density2, density1);
        });

        return copy;
    }

    private @NotNull List<LivingEntity> sortByMinWorry(@NotNull List<LivingEntity> copy) {
        copy.sort((entity1, entity2) -> {
            var density1 = readDoubleOnFriend(worryNamespacedKey, entity1);
            var density2 = readDoubleOnFriend(worryNamespacedKey, entity2);
            return Double.compare(density1, density2);
        });

        return copy;
    }


}
