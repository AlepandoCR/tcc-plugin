package tcc.gamers.ai.session;

import io.lumine.mythic.bukkit.utils.particles.Particle;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.DragonFlightMetricsHelper;
import tcc.gamers.ai.queue.ProcessorStrategy.DragonBrainMode;
import tcc.gamers.nms.nautilus.AutonomousDragonNautilus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A training session that keeps N autonomous dragons flying continuously
 * through random targets, feeding experience to the shared RSAC model.
 *
 */
public final class DragonTrainingSession extends DragonSession {

    private static final long MONITOR_PERIOD_TICKS = 5L;
    private static final long DRAGON_TIMEOUT_TICKS = 600L;

    private static final double MAX_WANDER_DISTANCE = 150.0;
    private static final double MAX_WANDER_DIST_SQ = MAX_WANDER_DISTANCE * MAX_WANDER_DISTANCE;

    private final int dragonCount;
    private final @NotNull Supplier<List<Location>> targetSupplier;
    private final @NotNull Location spawnLocation;


    private final Map<UUID, BukkitTask> timeoutTasks = new HashMap<>();
    private final Map<UUID, DragonFlightMetricsHelper> metricsHelpers = new HashMap<>();

    private @Nullable BukkitTask monitorTask;
    private final Random rng = new Random();
    private final TargetVisualizer targetVisualizer;


    public DragonTrainingSession(
            @NotNull TCCPlugin plugin,
            int dragonCount,
            @NotNull Location spawnLocation,
            @NotNull Supplier<List<Location>> targetSupplier
    ) {
        super(plugin);
        this.dragonCount    = dragonCount;
        this.spawnLocation  = spawnLocation;
        this.targetSupplier = targetSupplier;

        this.targetVisualizer = new TargetVisualizer(Material.LIME_STAINED_GLASS, 3.0f);
    }

    @Override
    protected @NotNull DragonBrainMode getMode() {
        return DragonBrainMode.TRAINING;
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < dragonCount; i++) {
            spawnDragon();
        }

        monitorTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::monitorArrivals,
                MONITOR_PERIOD_TICKS,
                MONITOR_PERIOD_TICKS
        );

        logger.info("[DragonTrainingSession] Started with " + dragonCount
                + " dragons. Timeout: " + DRAGON_TIMEOUT_TICKS + " ticks.");
    }

    @Override
    protected void onStop() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }

        timeoutTasks.values().forEach(BukkitTask::cancel);
        timeoutTasks.clear();

        stopAndRemoveAllDragons();
        targetVisualizer.clearAll();

        logger.info("[DragonTrainingSession] Stopped. All dragons removed.");
    }

    private void monitorArrivals() {
        var snapshot = new ArrayList<>(getActiveDragons());

        for (var dragon : snapshot) {
            if (dragon.hasArrived()) {
                handleTargetReached(dragon);
                continue;
            }

            //  Out of Bounds Check
            Location currentLoc = dragon.getLocation();
            if (currentLoc.getWorld().equals(spawnLocation.getWorld())) {
                if (currentLoc.distanceSquared(spawnLocation) > MAX_WANDER_DIST_SQ) {
                    logger.warning("[DragonTrainingSession] Dragon " + dragon.getUUID()
                            + " flew out of bounds (lost). Forcing respawn.");
                    // Treat out of bounds as a failure/timeout for the metrics
                    replaceDragon(dragon.getUUID(), false, true);
                }
            }
        }
    }


    private void handleTargetReached(@NotNull AutonomousDragonNautilus dragon) {
        logger.info("[DragonTrainingSession] Dragon " + dragon.getUUID()
                + " arrived at target. Replacing.");
        replaceDragon(dragon.getUUID(), false, false);

        var target = dragon.getLocationTarget().orElseThrow();
        plugin.getServer().broadcast(Component.text(String.format("[DragonTrainingSession] Dragón %s alcanzó su objetivo en X:%.1f Y:%.1f Z:%.1f",
                dragon.getUUID().toString().substring(0, 8),
                target.getX(), target.getY(), target.getZ())));

        target
                .getWorld()
                .spawnParticle(
                        Particle.FIREWORKS_SPARK.toBukkitParticle(),
                        target,
                        10,
                        0.5, 0.5,
                        0.5,
                        0.05
                );
    }


    private void spawnDragon() {
        var dragon = new AutonomousDragonNautilus(
                spawnLocation.getWorld(),
                plugin,
                manager
        );

        dragon.spawn(spawnLocation);
        assignRandomTarget(dragon);

        dragon.getLocationTarget().ifPresent(targetLocation -> {
            var bukkitDragon = dragon.getBukkitNautilus();

            Location dragonLocation = bukkitDragon.getLocation();
            Vector direction = targetLocation.toVector().subtract(dragonLocation.toVector()).normalize();

            float yaw = (float) Math.toDegrees(-Math.atan2(direction.getX(), direction.getZ()));
            float pitch = (float) Math.toDegrees(-Math.asin(direction.getY()));

            Location newLocation = dragonLocation.clone();
            newLocation.setYaw(yaw);
            newLocation.setPitch(pitch);

            bukkitDragon.teleport(newLocation);
        });

        var bukkitDragon = dragon.getBukkitNautilus();
        bukkitDragon.setRemoveWhenFarAway(false);

        registerDragon(dragon);

        if (DRAGON_TIMEOUT_TICKS > 0) {
            scheduleTimeout(dragon.getUUID());
        }

        logger.info("[DragonTrainingSession] Spawned dragon " + dragon.getUUID()
                + " → target " + dragon.getLocationTarget().map(Location::toString).orElse("none"));
    }

    private void replaceDragon(@NotNull UUID uuid, boolean timedOut, boolean outOfBounds) {

        if (timedOut){
            manager.getModelMetrics().notifyTimeout();
        }
        if (outOfBounds) manager.getModelMetrics().notifyOutOfBounds();

        cancelTimeout(uuid);
        targetVisualizer.clearTarget(uuid);
        removeDragon(uuid);
        spawnDragon();
    }


    private void assignRandomTarget(@NotNull AutonomousDragonNautilus dragon) {
        var targets = targetSupplier.get();

        if (targets == null || targets.isEmpty()) {
            logger.warning("[DragonTrainingSession] Target supplier returned empty list! "
                    + "Dragon " + dragon.getUUID() + " will idle until timeout.");
            return;
        }

        var target = targets.get(rng.nextInt(targets.size()));
        dragon.setTargetLocation(target);

        // Render target visualizer
        targetVisualizer.showTarget(dragon.getUUID(), target);
    }

    private void scheduleTimeout(@NotNull UUID uuid) {
        var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (getDragon(uuid) == null) return;

            logger.info("[DragonTrainingSession] Dragon " + uuid
                    + " timed out after " + DRAGON_TIMEOUT_TICKS + " ticks. Replacing.");
            replaceDragon(uuid, true, false);
        }, DRAGON_TIMEOUT_TICKS);

        timeoutTasks.put(uuid, task);
    }

    private void cancelTimeout(@NotNull UUID uuid) {
        var task = timeoutTasks.remove(uuid);
        if (task != null) task.cancel();
    }


    public void setDragonTarget(@NotNull UUID uuid, @NotNull Location target) {
        var dragon = getDragon(uuid);
        if (dragon == null) {
            logger.warning("[DragonTrainingSession] setDragonTarget: no dragon with UUID " + uuid);
            return;
        }
        dragon.setTargetLocation(target);
        targetVisualizer.showTarget(uuid, target);
    }

    public int getPendingTimeoutCount() {
        return timeoutTasks.size();
    }
}