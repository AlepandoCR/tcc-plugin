package tcc.gamers.ai.session;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.queue.ProcessorStrategy.DragonBrainMode;

import java.util.List;

/**
 * A race session where N dragons compete through a fixed ordered checkpoint sequence
 * using the frozen (inference-only) shared model
 */
public final class DragonRaceSession extends DragonSession {

    private final @NotNull List<Location> checkpoints;

    private final @NotNull List<Location> spawnPoints;

    /**
     * @param plugin      Plugin instance.
     * @param checkpoints Ordered checkpoint locations for the racecourse.
     * @param spawnPoints One spawn point per dragon participant.
     */
    public DragonRaceSession(
            @NotNull TCCPlugin plugin,
            @NotNull List<Location> checkpoints,
            @NotNull List<Location> spawnPoints
    ) {
        super(plugin);
        this.checkpoints = checkpoints;
        this.spawnPoints = spawnPoints;
    }

    @Override
    protected @NotNull DragonBrainMode getMode() {
        return DragonBrainMode.INFERENCE;
    }

    @Override
    protected void onStart() {
        throw new UnsupportedOperationException("DragonRaceSession.onStart() not yet implemented.");
    }

    @Override
    protected void onStop() {
        stopAndRemoveAllDragons();
    }
}