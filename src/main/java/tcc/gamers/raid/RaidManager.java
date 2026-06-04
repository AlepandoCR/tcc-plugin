package tcc.gamers.raid;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.data.AreaDto;
import tcc.gamers.data.DataDrivenManager;
import tcc.gamers.data.RaidDto;
import tcc.gamers.area.Area;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class RaidManager {

    private final TCCPlugin plugin;
    private List<Raid> raids = new ArrayList<>();

    public RaidManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        DataDrivenManager dataDrivenManager = plugin.getDataDrivenManager();

        for (Raid oldRaid : this.raids) {
            oldRaid.stopRaidTickLogic();
            oldRaid.stop();
        }

        List<Raid> newRaids = new ArrayList<>();

        for (RaidDto raidDto : dataDrivenManager.getRaids()) {
            dataDrivenManager.getArea(raidDto.getAreaIdentifier())
                    .flatMap(AreaDto::buildArea)
                    .ifPresent(area -> {
                        try {
                            var raid = new Raid(raidDto, area, plugin);
                            plugin.getLogger().fine("loaded raid with area ID: " + raid.getIdentifier());
                            newRaids.add(raid);

                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    });
        }

        plugin.getLogger().fine("loaded " + newRaids.size() + " raids");

        this.raids = Collections.unmodifiableList(newRaids);
    }

    public @NotNull Collection<Raid> getRaids() {
        return this.raids;
    }

    public @NotNull Optional<Raid> getRaid(@NotNull UUID raidId) {
        return raids.stream()
                .filter(raid -> raid.getRaidUniqueIdentifier().equals(raidId))
                .findFirst();
    }
}