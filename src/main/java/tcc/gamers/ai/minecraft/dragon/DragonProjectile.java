package tcc.gamers.ai.minecraft.dragon;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.nms.nautilus.DragonMountNautilus;
import tcc.gamers.tutorials.model.Model;

import java.util.Collection;

public class DragonProjectile extends BukkitRunnable {

    private final TCCPlugin plugin;
    private final DragonMountNautilus shooter;
    private final Model<ItemDisplay> model;
    
    private final Location currentLocation;
    private final Vector velocity;
    private final double damage;
    
    private int ticksLived = 0;

    public DragonProjectile(
            @NotNull TCCPlugin plugin,
            @NotNull DragonMountNautilus shooter,
            @NotNull Location startLocation,
            @NotNull LivingEntity target,
            double speed,
            double damage
    ) {
        this.plugin = plugin;
        this.shooter = shooter;
        this.currentLocation = startLocation.clone();
        this.damage = damage;

        // Calculate normalized directional vector from spawn to target core
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        this.velocity = targetLoc.toVector().subtract(this.currentLocation.toVector()).normalize().multiply(speed);

        // Spawn the display entity
        this.model = Model.spawn("dragon_projectile", this.currentLocation, ItemDisplay.class);
    }

    public void shoot() {
        this.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void run() {
        // 5 seconds max to prevent memory leaks
        int maxLifespan = 100;
        if (ticksLived++ > maxLifespan) {
            destroy();
            return;
        }

        // RayTrace to detect collisions before moving to prevent phasing through targets
        double hitBoxSize = 0.5;
        RayTraceResult hitResult = currentLocation.getWorld().rayTrace(
                currentLocation,
                velocity,
                velocity.length(),
                FluidCollisionMode.NEVER,
                true,
                hitBoxSize,
                entity -> {
                    if (entity.getUniqueId().equals(model.getEntity().getUniqueId())) return false;
                    if (entity.getUniqueId().equals(shooter.getBukkitNautilus().getUniqueId())) return false;
                    if (entity.getUniqueId().equals(shooter.getBukkitOwner().getUniqueId())) return false;

                    return entity instanceof LivingEntity;
                }
        );

        if (hitResult != null) {
            handleCollision(hitResult);
            return;
        }

        // Apply linear velocity
        currentLocation.add(velocity);
        
        // Update model position and add some visual flair during flight
        model.teleport(currentLocation);
        currentLocation.getWorld().spawnParticle(Particle.FLAME, currentLocation, 2, 0.1, 0.1, 0.1, 0.01);
    }

    private void handleCollision(@NotNull RayTraceResult hit) {
        Location hitLoc = hit.getHitPosition().toLocation(currentLocation.getWorld());

        // Spawn explosion effects visually and acoustically
        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 1);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Apply Area of Effect (AoE) damage within a 4.0 block radius
        double radius = 4.0;
        Collection<Entity> nearbyEntities = hitLoc.getWorld().getNearbyEntities(
                hitLoc, radius, radius, radius,
                entity -> entity instanceof LivingEntity
                        && !entity.getUniqueId().equals(shooter.getBukkitNautilus().getUniqueId())
        );

        for (Entity entity : nearbyEntities) {
            if(entity instanceof Monster monster){
                // Scale damage based on distance from the explosion epicenter
                double distance = monster.getLocation().distance(hitLoc);
                if (distance <= radius) {
                    // Closer entities take more damage. Minimum 1.0 damage at the edge.
                    double calculatedDamage = Math.max(1.0, damage - distance);
                    monster.damage(calculatedDamage, shooter.getBukkitNautilus());
                }
            }
        }

        destroy();
    }

    private void destroy() {
        this.cancel(); // Stop BukkitRunnable ticking
        model.despawn(); // Cleanup BetterModel tracker and Bukkit entity securely
    }
}