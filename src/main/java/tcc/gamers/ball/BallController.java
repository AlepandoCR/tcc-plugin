package tcc.gamers.ball;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ball.physics.KickController;
import tcc.gamers.ball.physics.PhysicManager;

import java.util.Optional;
import java.util.UUID;

public class BallController {

    private final LivingEntity nmsBall;
    private final org.bukkit.entity.LivingEntity bukkitBall;
    private final PhysicManager physicManager;
    private final KickController kickController;
    
    @Nullable
    private UUID lastInteractedEntity = null;

    public BallController(@NotNull TCCPlugin plugin, @NotNull LivingEntity nmsBall) {
        this.nmsBall = nmsBall;
        this.bukkitBall = (org.bukkit.entity.LivingEntity) nmsBall.getBukkitEntity();

        this.physicManager = new PhysicManager(this, plugin);
        this.kickController = new KickController(plugin, this);
    }

    public void tick() {
        physicManager.calculateBallPhysics(nmsBall);
    }

    public void setLastInteractedEntity(@Nullable UUID uuid) {
        this.lastInteractedEntity = uuid;
    }

    public @NotNull Optional<UUID> getLastInteractedEntity() {
        return Optional.ofNullable(lastInteractedEntity);
    }

    public @NotNull org.bukkit.entity.LivingEntity getBukkitBall() {
        return bukkitBall;
    }

    public @NotNull KickController getKickController() {
        return kickController;
    }

    public @NotNull PhysicManager getPhysicManager() {
        return physicManager;
    }
}