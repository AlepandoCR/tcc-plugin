package tcc.gamers.ai.session;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.queue.DragonQueueManager;
import tcc.gamers.ai.queue.ProcessorStrategy.DragonBrainMode;
import tcc.gamers.nms.nautilus.AutonomousDragonNautilus;
import org.spartan.api.exception.SpartanNativeException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Abstract base for any dragon AI session
 */
public abstract class DragonSession {

    protected final @NotNull TCCPlugin plugin;

    protected final @NotNull Logger logger;

    protected final @NotNull DragonQueueManager manager;

    private boolean running = false;

    private @Nullable BukkitTask processTickTask;


    private final Map<UUID, AutonomousDragonNautilus> dragons = new LinkedHashMap<>();


    protected DragonSession(@NotNull TCCPlugin plugin) {
        this.plugin  = plugin;
        this.logger  = plugin.getLogger();
        this.manager = new DragonQueueManager(plugin, getMode());
    }


    /**
     * @return The brain mode for this session type.
     * Training sessions return {@link DragonBrainMode#TRAINING};
     * race/inference sessions return {@link DragonBrainMode#INFERENCE}.
     */
    @NotNull
    protected abstract DragonBrainMode getMode();

    /**
     * Called by {@link #start()} after the manager is initialized and the
     * processTick task is scheduled. Subclasses spawn dragons and set up
     * their session-specific monitoring here.
     */
    protected abstract void onStart();

    /**
     * Called by {@link #stop()} before the processTick task is canceled and
     * before the manager shuts down. Subclasses must:
     */
    protected abstract void onStop();

    /**
     * Initializes the model, schedules the tick processor, and delegates to
     * {@link #onStart()} for session-specific setup.
     */
    public final void start() {
        if (running) {
            logger.warning("[DragonSession] start() called but session is already running.");
            return;
        }

        try {
            manager.initialize();
        } catch (SpartanNativeException e) {
            logger.severe("[DragonSession] Failed to initialize model, session will not start: " + e.getMessage());
            return;
        }

        processTickTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, manager::processTick, 1L, 1L);

        running = true;
        logger.info("[DragonSession] Session started (" + getClass().getSimpleName() + ", mode=" + getMode() + ").");
        onStart();
    }

    /**
     * Tears down the session: delegates cleanup to {@link #onStop()}, cancels
     * the tick processor, and shuts down the model (save -> close -> null).
     *
     * <p>No-op if the session is not running.</p>
     */
    public final void stop() {
        if (!running) {
            logger.warning("[DragonSession] stop() called but session is not running.");
            return;
        }

        running = false;

        // Let the subclass clean up dragons and per-dragon tasks first.
        onStop();

        // Cancel the central processTick task.
        if (processTickTask != null) {
            processTickTask.cancel();
            processTickTask = null;
        }

        // Save weights, close native resources.
        manager.shutdown();

        logger.info("[DragonSession] Session stopped (" + getClass().getSimpleName() + ").");
    }

    /** @return {@code true} if the session is currently active. */
    public final boolean isRunning() {
        return running;
    }

    /**
     * Registers a dragon into the session registry and starts it.
     * Call this from {@link #onStart()} or from the monitoring task when
     * spawning a replacement dragon.
     *
     * @param dragon A fully spawned dragon, ready to receive targets.
     */
    protected final void registerDragon(@NotNull AutonomousDragonNautilus dragon) {
        dragons.put(dragon.getUUID(), dragon);
        dragon.start();
    }

    /**
     * Stops the dragon, removes it from the registry, and despawns the NMS entity.
     * Safe to call from within the monitoring task (same main thread).
     *
     * @param uuid The UUID of the dragon to remove.
     */
    protected final void removeDragon(@NotNull UUID uuid) {
        var dragon = dragons.remove(uuid);
        if (dragon == null) return;

        dragon.stop();
        dragon.getBukkitEntity().remove();
    }

    /**
     * Stops and despawns all currently registered dragons.
     * Subclasses must call this (or equivalent per-dragon removal) inside
     * {@link #onStop()} to guarantee a clean world state after the session ends.
     */
    protected final void stopAndRemoveAllDragons() {
        // Snapshot keys to avoid ConcurrentModificationException while iterating.
        new java.util.ArrayList<>(dragons.keySet()).forEach(this::removeDragon);
    }

    /**
     * Looks up a specific dragon by UUID.
     *
     * @param uuid The entity UUID.
     * @return The dragon, or {@code null} if not registered in this session.
     */
    public @Nullable AutonomousDragonNautilus getDragon(@NotNull UUID uuid) {
        return dragons.get(uuid);
    }

    /**
     * @return An unmodifiable view of all currently active dragons in this session.
     *         Useful for admin commands that iterate all dragons.
     */
    public @NotNull Collection<AutonomousDragonNautilus> getActiveDragons() {
        return Collections.unmodifiableCollection(dragons.values());
    }

    /** @return How many dragons are currently active in this session. */
    public int getActiveDragonCount() {
        return dragons.size();
    }

    @NotNull
    public DragonQueueManager getManager() {
        return manager;
    }
}