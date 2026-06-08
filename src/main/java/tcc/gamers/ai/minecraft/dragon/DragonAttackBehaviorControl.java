package tcc.gamers.ai.minecraft.dragon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.nms.nautilus.CustomNautilus;
import tcc.gamers.nms.nautilus.NautilusAnimationManager;

import java.util.Comparator;
import java.util.Set;

public class DragonAttackBehaviorControl<E extends CustomNautilus> implements BehaviorControl<E> {

    private final TCCPlugin plugin;
    private @NotNull Behavior.Status status = Behavior.Status.STOPPED;

    // Attack cooldown management
    private long lastAttackTime = 0;
    private final long attackCooldownMillis = 10000; // 10 seconds between shots
    private boolean isOnCooldown = false;

    public DragonAttackBehaviorControl(@NotNull TCCPlugin plugin) {
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
        return bool; // Cannot act on its own if ridden
    }

    @Override
    public void tickOrStop(@NotNull ServerLevel serverLevel, @NotNull E e, long l) {
        if (!tryStart(serverLevel, e, l)) {
            doStop(serverLevel, e, l);
            return;
        }

        var bukkitEntity = e.getBukkitNautilus();

        var nearHostile = bukkitEntity.getLocation().getNearbyEntitiesByType(Monster.class, CustomNautilus.DRAGON_ATTACK_DISTANCE);
        if (nearHostile.isEmpty()) {
            status = Behavior.Status.STOPPED;
            return;
        }

        var target = nearHostile.stream()
                .min(Comparator.comparingDouble(e2 -> e2.getLocation().distanceSquared(bukkitEntity.getLocation())))
                .orElse(null);

        e.getLookControl().setLookAt(target.getX(), target.getEyeHeight(), target.getZ());

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= attackCooldownMillis) {
            isOnCooldown = false;
            Location headLoc = bukkitEntity.getLocation().add(0, bukkitEntity.getHeight() * 0.8, 0);
            Vector directionToTarget = target.getLocation().toVector().subtract(headLoc.toVector()).normalize();

            // Multiply by a smaller margin (e.g., 0.8) to avoid spawning inside terrain hills
            Location spawnLoc = headLoc.clone().add(directionToTarget.multiply(0.8));

            // If the calculated spawn location is inside a solid block, shift it upwards
            if (spawnLoc.getBlock().getType().isSolid()) {
                spawnLoc.add(0, 1.2, 0);
            }

            // Spawn and shoot the projectile
            DragonProjectile projectile = new DragonProjectile(plugin, e, spawnLoc, target, 1.5, 6.0);
            projectile.shoot();
            e.getAnimationManager().play(NautilusAnimationManager.Animation.ATTACK);

            lastAttackTime = currentTime;
        }{
            isOnCooldown = true;
        }
    }

    public boolean isOnCooldown() {
        return isOnCooldown;
    }

    @Override
    public void doStop(@NotNull ServerLevel serverLevel, @NotNull E e, long l) {
        status = Behavior.Status.STOPPED;
    }

    @Override
    public @NotNull String debugString() {
        return "Dragon_Attack_Behavior";
    }
}