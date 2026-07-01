package tcc.gamers.event.race;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.race.Race;

import java.util.UUID;

public class EntityCrossCheckpointEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Race<?> race;
    private final @NotNull UUID entityUuid;
    private final @NotNull Location checkpointLocation;
    private final int checkpointIndex;
    private final boolean isGoal;

    public EntityCrossCheckpointEvent(
            @NotNull Race<?> race,
            @NotNull UUID entityUuid,
            @NotNull Location checkpointLocation,
            int checkpointIndex,
            boolean isGoal
    ) {
        this.race = race;
        this.entityUuid = entityUuid;
        this.checkpointLocation = checkpointLocation;
        this.checkpointIndex = checkpointIndex;
        this.isGoal = isGoal;
    }

    public @NotNull Race<?> getRace() { return race; }
    public @NotNull UUID getEntityUuid() { return entityUuid; }
    public @NotNull Location getCheckpointLocation() { return checkpointLocation; }
    public int getCheckpointIndex() { return checkpointIndex; }
    public boolean isGoal() { return isGoal; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}