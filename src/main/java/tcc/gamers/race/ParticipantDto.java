package tcc.gamers.race;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record ParticipantDto(@NotNull UUID uuid, @NotNull List<Integer> crossedCheckpoints, double distance) {

    public int getLastCheckpointIndex() {
        return crossedCheckpoints.isEmpty() ? -1 : crossedCheckpoints.getLast();
    }
}