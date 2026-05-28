package tcc.gamers.ai.minecraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spartan.api.engine.SpartanModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Integrates the Spartan AI engine into Minecraft's native entity brain system.
 * <p>
 * This behavior control handles the execution ticking and periodic persistence
 * of the Spartan model.
 *
 * @param <E> The type of living entity controlled by this behavior.
 */
public class SpartanBrainTickerBehaviorControl<E extends LivingEntity> implements BehaviorControl<E> {

    private final SpartanModel<?> model;

    private final int tickFrequency;
    private final int saveIntervalTicks;

    @Nullable
    private final Path savePath;

    private long currentTick = 0;

    /**
     * Constructs a new Spartan brain ticker.
     *
     * @param model             The Spartan model to execute.
     * @param tickFrequency     How often the model should evaluate (e.g., 5 = every 5 Minecraft ticks).
     * @param saveIntervalTicks How often the model should be saved to disk (e.g., 6000 = 5 minutes).
     * @param savePath          The file path to save the model, or null to disable auto-saving.
     */
    public SpartanBrainTickerBehaviorControl(
            @NotNull SpartanModel<?> model,
            int tickFrequency,
            int saveIntervalTicks,
            @Nullable Path savePath
    ) {
        this.model = model;
        this.tickFrequency = Math.max(1, tickFrequency);
        this.saveIntervalTicks = Math.max(20, saveIntervalTicks); // Minimum 1 second
        this.savePath = savePath;
    }

    @Override
    public @NotNull Behavior.Status getStatus() {
        return Behavior.Status.RUNNING;
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public boolean tryStart(
            @NotNull ServerLevel serverLevel,
            @NotNull E livingEntity,
            long gameTime
    ) {
        model.getSpartanContext().update();
        return true;
    }

    @Override
    public void tickOrStop(
            @NotNull ServerLevel serverLevel,
            @NotNull E livingEntity,
            long gameTime
    ) {
        // Model Evaluation Phase
        if (currentTick % tickFrequency == 0) {
            model.getSpartanContext().update();
            model.tick();
        }

        // Persistence Phase (Decoupled from execution frequency)
        if (savePath != null && currentTick > 0 && currentTick % saveIntervalTicks == 0) {
            saveModelSafely();
        }

        currentTick++;
    }

    @Override
    public void doStop(
            @NotNull ServerLevel serverLevel,
            @NotNull E livingEntity,
            long gameTime
    ) {
        // Ensure we save the latest state before shutting down the behavior
        if (savePath != null) {
            saveModelSafely();
        }
        model.close();
    }

    /**
     * Attempts to persist the model to the configured path.
     * <p>
     * Note: Disk I/O blocks the main thread. If the Spartan model becomes large,
     * consider offloading this to an asynchronous thread if Spartan's API is thread-safe.
     */
    private void saveModelSafely() {
        try {
            if(savePath != null) {
                Path parent = savePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                model.saveModel(savePath);
            }
        } catch (IOException | RuntimeException e) {
            // Silently fail to prevent crashing the entity's tick loop,
            // but you might want to log this to a severe error file.
            System.err.println("[Spartan AI] Failed to save model: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void loadModel(@NotNull Path path) {
        model.loadModel(path);
    }

    @SuppressWarnings("unused")
    public void saveModel(@NotNull Path path) {
        model.saveModel(path);
    }

    @Override
    public @NotNull String debugString() {
        return "spartan_brain_ticker";
    }
}