package tcc.gamers.area;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class AreaManager {

    private final @NotNull TCCPlugin plugin;

    private final @NotNull Collection<Area> areas;

    public AreaManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.areas = ConcurrentHashMap.newKeySet();
    }

    public @NotNull Collection<Area> getAreas() {
        return areas;
    }

    public void addArea(@NotNull Area area) {
        areas.add(area);
    }

    public void removeArea(@NotNull Area area) {
        areas.remove(area);
    }

    /**
     * @implNote clears all matches
     * @param identifier the area identifier {@link Area#getIdentifier()}
     */
    public void removeArea(@NotNull String identifier) {
        areas.stream()
                .filter(area ->
                        area.getIdentifier()
                                .equals(identifier)
                ).forEach(this::removeArea);
    }

    public <AreaType extends Area> void convertAreaTypeArea(@NotNull AreaType typedArea) {
        areas.removeIf(area -> area.getIdentifier().equalsIgnoreCase(typedArea.getIdentifier())); // remove any matches with the ID

        areas.add(typedArea); // add again as typed
    }

    /**
     * @implNote clears all matches
     * @param identifier the supervised area identifier {@link SupervisedArea#getIdentifier()}
     */
    public void startSupervisedArea(@NotNull String identifier) {
        getAreasOfType(SupervisedArea.class).forEach(SupervisedArea::start);
    }

    /**
     * @implNote clears all matches
     * @param identifier the supervised area identifier {@link SupervisedArea#getIdentifier()}
     */
    public void stopSupervisedArea(@NotNull String identifier) {
        getAreasOfType(SupervisedArea.class).forEach(SupervisedArea::stop);
    }

    public <AreaType extends Area> @NotNull Collection<AreaType> getAreasOfType(@NotNull Class<AreaType> areaTypeClass){
        var newList = new ArrayList<AreaType>();
        areas.stream()
                .filter(areaTypeClass::isInstance)
                .map(areaTypeClass::cast)
                .forEach(newList::add);
        return newList;
    }
}
