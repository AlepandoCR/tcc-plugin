package tcc.gamers.race;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.area.SupervisedArea;
import tcc.gamers.race.dragon.DragonRace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RaceManager {
    private final TCCPlugin plugin;
    private final Map<String, Race<?>> activeRaces = new ConcurrentHashMap<>();

    public RaceManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(){
        activeRaces.values().forEach(SupervisedArea::stop);
        activeRaces.clear();
        plugin.getDataDrivenManager().reload().thenAccept( _-> plugin.getDataDrivenManager().getDragonRaceConfigs().forEach((id, config) -> {

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                var path = plugin.getRacePathManager().getLoadedPath(id);

                if(path != null){
                    registerRace(new DragonRace(plugin, path, config));
                }
            });
        }));
    }

    public void registerRace(@NotNull Race<?> race) {
        this.activeRaces.put(race.getIdentifier(), race);
        race.start();
    }

    public void unregisterRace(@NotNull String identifier) {
        this.activeRaces.remove(identifier);
    }

    public @NotNull Optional<Race<?>> getRace(@NotNull String identifier) {
        return Optional.ofNullable(activeRaces.get(identifier));
    }

    public <T extends Race<?>> @NotNull List<T> getRacesOfType(@NotNull Class<T> raceClass) {
        List<T> filteredRaces = new ArrayList<>();
        for (Race<?> race : activeRaces.values()) {
            if (raceClass.isInstance(race)) {
                filteredRaces.add(raceClass.cast(race));
            }
        }
        return filteredRaces;
    }

    public @NotNull Collection<Race<?>> getActiveRaces() {
        return activeRaces.values();
    }

    public @NotNull Optional<Race<?>> getRaceForParticipant(@NotNull UUID uuid){
        return getActiveRaces().stream().filter(race -> race.isParticipant(uuid)).findFirst();
    }

    public void clear() {
        this.activeRaces.clear();
    }
}