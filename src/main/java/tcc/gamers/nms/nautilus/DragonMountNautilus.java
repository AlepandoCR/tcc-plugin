package tcc.gamers.nms.nautilus;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.minecraft.dragon.DragonAttackBehaviorControl;
import tcc.gamers.ai.minecraft.dragon.DragonFollowOwnerBehaviorControl;

import java.util.Optional;
import java.util.Set;

/**
 * Concrete player-mounted dragon Nautilus.
 *
 * <p>Extends {@link AbstractDragonNautilus} and owns everything specific to
 * the player-riding use case: the owner reference, the HUD task, the follow
 * and attack Brain activities, and the despawn policy based on owner distance.</p>
 *
 * <p>The physics contract is simple: every tick, {@link #tickController()}
 * extracts the look vector and pitch from the NMS {@link Player} and feeds
 * them into {@link #onRiderLook(Vec3, float)} / {@link #onRiderJump(Vec3, float)}.
 * All math lives in {@link AbstractDragonNautilus}.</p>
 *
 * <h3>For a PPO-controlled dragon</h3>
 * Extend {@link AbstractDragonNautilus} directly, implement
 * {@link #tickController()}, and call {@link #applyFlight(Vec3, float)} with
 * the policy output — no player or mount layer needed.
 */
public class DragonMountNautilus extends AbstractDragonNautilus<DragonMountNautilus> {


    public static final int MAX_OWNER_DISTANCE     = 25;
    public static final int OWNER_FOLLOW_DISTANCE  = 3;
    public static final int DRAGON_ATTACK_DISTANCE = 16;


    private final @NotNull org.bukkit.entity.Player owner;
    private final @NotNull DragonAttackBehaviorControl<DragonMountNautilus> attackBehaviorControl;
    private final @NotNull DragonUITask dragonUITask;

    private boolean wasVehicleLastTick = false;

    public DragonMountNautilus(
            @NotNull World world,
            @NotNull TCCPlugin plugin,
            @NotNull org.bukkit.entity.Player owner
    ) {
        super(world, plugin);
        this.owner                 = owner;
        this.attackBehaviorControl = new DragonAttackBehaviorControl<>(plugin);
        this.dragonUITask          = new DragonUITask(plugin, this, owner);
    }


    @Override
    void onSpawn(@NotNull Location spawnLocation) {
        // nothing
    }

    @Override
    protected void registerBrainActivities(@NotNull Brain<DragonMountNautilus> brain) {
        brain.addActivity(
                Activity.CORE,
                ImmutableList.of(
                        Pair.of(1, new DragonFollowOwnerBehaviorControl<>(plugin)),
                        Pair.of(2, attackBehaviorControl)
                ),
                Set.of(),
                Set.of()
        );
    }

    @Override
    void registerExtraGoals(@NotNull GoalSelector goalSelector) {

    }


    @Override
    public void tick() {
        boolean isVehicleNow = this.isVehicle();

        if (isVehicleNow && !wasVehicleLastTick) {
            dragonUITask.start();
        } else if (!isVehicleNow && wasVehicleLastTick) {
            dragonUITask.stop();
        }
        wasVehicleLastTick = isVehicleNow;

        super.tick();
    }



    @Override
    protected final void tickRidden(@NotNull Player controller, @NotNull Vec3 riddenInput) {
        this.playerJumpPendingScale = 0.0F;
    }

    @Override
    protected void tickController() {
        if (!(this.getControllingPassenger() instanceof Player player)) return;


        float yRot = player.getYRot();
        float xRot = player.getXRot();

        this.setYRot(yRot);
        this.setXRot(xRot);
        this.yBodyRot = yRot;
        this.yHeadRot = yRot;

        if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
            onRiderJump(player.getLookAngle(), this.playerJumpPendingScale);
        }

        onRiderLook(player.getLookAngle(), xRot);
    }


    /**
     * Apply one tick of flight from the given look vector and pitch.
     * Routes through dash if one is active.
     */
    protected final void onRiderLook(@NotNull Vec3 lookAngle, float pitch) {
        if (isDashing()) {
            tickDash();
        } else {
            applyFlight(lookAngle, pitch);
        }
    }

    /**
     * Initiate a dash toward the given direction with the given jump pressure.
     */
    protected final void onRiderJump(@NotNull Vec3 lookDir, float jumpScale) {
        initiateDash(lookDir, jumpScale);
    }

    @Override
    protected boolean shouldDespawn() {
        return !owner.isValid()
                || !owner.isOnline()
                || owner.getWorld() != getBukkitNautilus().getWorld()
                || owner.getLocation().distance(getBukkitNautilus().getLocation()) > MAX_OWNER_DISTANCE;
    }

    public @NotNull org.bukkit.entity.Player getBukkitOwner() {
        return owner;
    }

    public @NotNull DragonAttackBehaviorControl<DragonMountNautilus> getAttackBehaviorControl() {
        return attackBehaviorControl;
    }

    public @NotNull Optional<DragonUITask> getDragonUITask() {
        return Optional.of(dragonUITask);
    }
}