package tcc.gamers.ball.dash;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Owns all active {@link DashCooldown} instances and performs the
 * dash push itself (velocity burst + crawling visual feedback).
 */
public class DashManager {

    private static final double DASH_TARGET_DISTANCE = 5.5;
    private static final double DASH_SPEED_MULTIPLIER = 7.0;
    private static final double MIN_DASH_SPEED = 0.13;
    private static final int DASH_PUSH_TIMEOUT_TICKS = 60;
    private static final int CRAWL_POSE_TICKS = 15;

    private final @NotNull List<DashCooldown> cooldowns;

    private final @NotNull TCCPlugin plugin;

    public DashManager(@NotNull TCCPlugin plugin) {
        this.cooldowns = new ArrayList<>();
        this.plugin = plugin;
    }

    /**
     * Attempts to dash the player in their current facing direction.
     * No-ops (with feedback) if the player is still on cooldown.
     */
    public void tryDash(@NotNull Player player) {
        DashCooldown existing = findCooldown(player.getUniqueId());

        if (existing != null) {
            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS,
                    100f,
                    0.5f
            );
            return;
        }

        var cooldown = new DashCooldown(player, plugin, () -> removeCooldown(player.getUniqueId()));
        cooldown.start();
        cooldowns.add(cooldown);

        pushPlayerInCurrentDirection(player);
    }

    public boolean isOnCooldown(@NotNull Player player) {
        return findCooldown(player.getUniqueId()) != null;
    }

    public void removeCooldown(@NotNull UUID playerUniqueIdentifier) {
        var cooldown = findCooldown(playerUniqueIdentifier);
        if (cooldown != null) {
            cooldown.stop();
            cooldowns.remove(cooldown);
        }
    }

    private @Nullable DashCooldown findCooldown(@NotNull UUID playerUniqueIdentifier) {
        return cooldowns.stream()
                .filter(cooldown -> cooldown.getPlayer().getUniqueId().equals(playerUniqueIdentifier))
                .findFirst()
                .orElse(null);
    }

    private void pushPlayerInCurrentDirection(@NotNull Player player) {
        Vector direction = player.getLocation().getDirection().normalize();

        if (
                player.getLocation()
                        .subtract(0, 1, 0)
                        .getBlock()
                        .getType()
                        .isSolid()
        ) {
            direction.setY(Math.max(0.2, direction.getY()));
        }

        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        Vector velocity = direction.multiply(
                Math.max(
                        MIN_DASH_SPEED,
                        movementSpeed.getValue()
                ) * DASH_SPEED_MULTIPLIER
        );
        Vector startLocation = player.getLocation().toVector();

        player.getScheduler().runAtFixedRate(
                plugin,
                new Consumer<>() {
                    double movedDistance = 0;
                    int elapsedTicks = 0;

                    @Override
                    public void accept(@NotNull ScheduledTask task) {
                        elapsedTicks++;

                        if (movedDistance >= DASH_TARGET_DISTANCE || elapsedTicks >= DASH_PUSH_TIMEOUT_TICKS) {
                            task.cancel();
                            return;
                        }

                        player.setVelocity(velocity);
                        player.getWorld().spawnParticle(
                                Particle.EXPLOSION,
                                player.getLocation(),
                                1
                        );
                        player.getWorld().spawnParticle(
                                Particle.CLOUD,
                                player.getLocation(),
                                20
                        );

                        setPlayerCrawling(player);

                        movedDistance = player.getLocation()
                                .toVector()
                                .distance(startLocation);
                    }
                },
                () -> {},
                1L,
                1L
        );
    }

    private void setPlayerCrawling(@NotNull Player player) {
        var craftPlayer = (CraftPlayer) player;
        var entity = craftPlayer.getHandle();

        player.getScheduler().runAtFixedRate(
                plugin,
                new Consumer<>() {
                    int ticks = 0;

                    @Override
                    public void accept(@NotNull ScheduledTask task) {
                        ticks++;
                        if (ticks >= CRAWL_POSE_TICKS) {
                            task.cancel();
                            return;
                        }

                        entity.setPose(Pose.SWIMMING);

                        List<SynchedEntityData.DataValue<?>> dataValues = entity.getEntityData().getNonDefaultValues();
                        if(dataValues == null) {
                            return;
                        }
                        var crawlingPacket = new ClientboundSetEntityDataPacket(
                                player.getEntityId(),
                                dataValues
                        );

                        entity.connection.send(crawlingPacket);
                    }
                },
                () -> {},
                1L,
                1L
        );
    }
}