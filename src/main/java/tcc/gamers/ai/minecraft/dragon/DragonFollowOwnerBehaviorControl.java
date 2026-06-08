package tcc.gamers.ai.minecraft.dragon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.nms.nautilus.CustomNautilus;
import tcc.gamers.nms.nautilus.NautilusAnimationManager;

import java.util.Set;

public class DragonFollowOwnerBehaviorControl<E extends CustomNautilus> implements BehaviorControl<E> {

    private final TCCPlugin plugin;

    private @NotNull Behavior.Status status = Behavior.Status.STOPPED;

    public DragonFollowOwnerBehaviorControl(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public Behavior.Status getStatus() {
        return status;
    }

    @NotNull
    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public boolean tryStart(@NotNull ServerLevel serverLevel, @NotNull E e, long l) {
        var bool = !e.hasControllingPassenger();
        status = bool ? Behavior.Status.RUNNING : Behavior.Status.STOPPED;
        return bool;
    }

    @Override
    public void tickOrStop(@NotNull ServerLevel serverLevel, @NotNull E e, long l) {

        if(!tryStart(serverLevel, e, l)){
            doStop(serverLevel, e, l);
            return;
        }



        var owner = e.getBukkitOwner();

        if(e.getBukkitNautilus().getLocation().distance(owner.getLocation()) < CustomNautilus.OWNER_FOLLOW_DISTANCE ){
            e.getNavigation().stop(); // close enough, stop nav
            e.getAnimationManager().play(NautilusAnimationManager.Animation.FLY_IDLE);
            return;
        }

        var x = owner.getX();
        var y = owner.getY();
        var z = owner.getZ();

        e.getNavigation().moveTo(x,y,z, CustomNautilus.OWNER_FOLLOW_DISTANCE,1.2); // follow owner
        e.getAnimationManager().play(NautilusAnimationManager.Animation.FLY_WALK);
    }

    @Override
    public void doStop(@NotNull ServerLevel serverLevel, @NotNull E e, long l) {
        e.getNavigation().stop(); // stop nav
    }

    @Override
    public @NotNull String debugString() {
        return "Dragon_Follow_Owner";
    }
}
