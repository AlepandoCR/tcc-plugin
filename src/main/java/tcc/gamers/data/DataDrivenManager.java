package tcc.gamers.data;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.DataDrivenLoader;
import tcc.gamers.util.StorageFolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Manager responsible for orchestrating, holding, and providing access to the
 * in-memory collections of all data-driven configurations.
 * * It registers directories into the {@link DataDrivenLoader} to parse files asynchronously
 * and builds efficient, thread-safe lookup maps using unique identifiers.
 */
@SuppressWarnings("unused")
public class DataDrivenManager {

    private final TCCPlugin plugin;

    private Map<String, AreaDto> areas = new ConcurrentHashMap<>();
    private Map<String, RaidDto> raids = new ConcurrentHashMap<>();
    private Map<String, RaidMobDto> raidMobs = new ConcurrentHashMap<>();

    /**
     * Constructs a new DataDrivenManager.
     *
     * @param plugin The core plugin instance
     */
    public DataDrivenManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public CompletableFuture<Void> reload() {
        DataDrivenLoader loader = new DataDrivenLoader(plugin);

        loader.loadDirectory(
                StorageFolder.RAID_AREA,
                GeneratedAreaDto.class,
                AreaDto::new,
                (List<AreaDto> loadedAreas) -> this.areas = loadedAreas.stream()
                        .collect(Collectors.toConcurrentMap(AreaDto::getAreaId, area -> area)),
                "Areas"
        );

        loader.loadDirectory(
                StorageFolder.RAID,
                GeneratedRaidDto.class,
                RaidDto::new,
                (List<RaidDto> loadedRaids) -> this.raids = loadedRaids.stream()
                        .collect(Collectors.toConcurrentMap(RaidDto::getAreaIdentifier, raid -> raid)),
                "Raids"
        );

        loader.loadDirectory(
                StorageFolder.RAID_MOB,
                GeneratedRaidMobDto.class,
                RaidMobDto::new,
                (List<RaidMobDto> loadedMobs) -> this.raidMobs = loadedMobs.stream()
                        .collect(Collectors.toConcurrentMap(RaidMobDto::getRaidMobId, mob -> mob)),
                "RaidMobs"
        );

        return loader.loadAll();
    }

    /**
     * Gets a loaded Area configuration by its unique identifier.
     *
     * @param areaId The identifier of the area
     * @return An Optional containing the AreaDto if found, otherwise empty
     */
    public @NotNull Optional<AreaDto> getArea(@NotNull String areaId) {
        return Optional.ofNullable(areas.get(areaId));
    }

    /**
     * Returns an unmodifiable collection of all currently loaded Area configurations.
     *
     * @return All loaded AreaDtos
     */
    public @NotNull Collection<AreaDto> getAreas() {
        return Collections.unmodifiableCollection(areas.values());
    }

    /**
     * Gets a loaded Raid configuration by its linked area identifier.
     *
     * @param areaIdentifier The linked area identifier acting as key
     * @return An Optional containing the RaidDto if found, otherwise empty
     */
    public @NotNull Optional<RaidDto> getRaid(@NotNull String areaIdentifier) {
        return Optional.ofNullable(raids.get(areaIdentifier));
    }

    /**
     * Returns an unmodifiable collection of all currently loaded Raid configurations.
     *
     * @return All loaded RaidDtos
     */
    public @NotNull Collection<RaidDto> getRaids() {
        return Collections.unmodifiableCollection(raids.values());
    }

    /**
     * Gets a custom Raid Mob configuration by its internal identifier.
     *
     * @param raidMobId The custom raid mob id
     * @return An Optional containing the RaidMobDto if found, otherwise empty
     */
    public @NotNull Optional<RaidMobDto> getRaidMob(@NotNull String raidMobId) {
        return Optional.ofNullable(raidMobs.get(raidMobId));
    }

    /**
     * Returns an unmodifiable collection of all currently loaded Raid Mob configurations.
     *
     * @return All loaded RaidMobDtos
     */
    public @NotNull Collection<RaidMobDto> getRaidMobs() {
        return Collections.unmodifiableCollection(raidMobs.values());
    }
}