package tcc.gamers.ball;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

public class Ball extends Armadillo {

    private final BallController controller;

    private final World world;

    private final TCCPlugin plugin;

    public Ball(@NotNull World world, @NotNull TCCPlugin plugin) {
        super(
                EntityType.ARMADILLO,
                ((CraftWorld) world).getHandle()
        );
        this.world = world;
        this.plugin = plugin;

        controller = new BallController(this.plugin, this);
    }

    public void spawn(@NotNull Location location) {
        this.setPos(location.getX(), location.getY(), location.getZ());
        this.setInvulnerable(true);
        this.setSilent(true);
        world.addEntity(this.getBukkitEntity());
    }

    @Override
    public void registerGoals() {
        // No goals for the ball
    }

    @Override
    public void tick() {
        super.tick();
        getBukkitEntity().getPersistentDataContainer().set(TCCNameSpacedKeys.SOCCER_BALL.getNamespacedKey(), PersistentDataType.BOOLEAN, true);
        controller.getPhysicManager().calculateBallPhysics(this);
        this.switchToState(ArmadilloState.SCARED); // setting the state makes noise, set to silent for that
    }

    /**
     * Creates a copy of this Ball instance, including its position and velocity.
     * Useful for a prop ball after a goal
     * @return A new Ball instance with the same state as this one.
     */
    @Contract("_ -> new")
    public @NotNull Ball copy(boolean sameState) {
        Ball clonedBall = new Ball(this.world, this.plugin);
        clonedBall.setPos(this.getX(), this.getY(), this.getZ());
        if(sameState){
            clonedBall.setDeltaMovement(this.getDeltaMovement());
        }
        return clonedBall;
    }

    /**
     * Creates a deep copy of this Ball instance and removes the original from the world.
     * @return A new Ball instance with the same state as this one, while the original is removed from the world.
     */
    @Contract("_ -> new")
    public @NotNull Ball copyAndDestroy(boolean sameState) {
        Ball clonedBall = copy(sameState);
        this.remove(RemovalReason.DISCARDED);
        return clonedBall;
    }

    public @NotNull BallController getController() {
        return controller;
    }


    public static boolean isBall(@NotNull org.bukkit.entity.Entity entity) {
        return entity.getPersistentDataContainer().has(TCCNameSpacedKeys.SOCCER_BALL.getNamespacedKey(), PersistentDataType.BOOLEAN);
    }


}
