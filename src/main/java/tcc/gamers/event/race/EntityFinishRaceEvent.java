package tcc.gamers.event.race;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.race.Race;

import java.util.UUID;

public class EntityFinishRaceEvent extends EntityCrossCheckpointEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int place;

    public EntityFinishRaceEvent(
            @NotNull Race<?> race,
            @NotNull UUID entityUuid,
            @NotNull Location checkpointLocation,
            int checkpointIndex,
            int place
    ) {
        super(race, entityUuid, checkpointLocation, checkpointIndex, true);
        this.place = place;
    }

    public int getPlace() {
        return place;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}