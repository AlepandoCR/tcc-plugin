package tcc.gamers.raid;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.data.AreaDto;
import tcc.gamers.data.DataDrivenManager;
import tcc.gamers.data.RaidDto;
import tcc.gamers.area.Area;

import java.util.*;

@SuppressWarnings("unused")
public class RaidManager {

    private final TCCPlugin plugin;

    private List<Raid> raidTemplates = new ArrayList<>();

    private final List<Raid> activeRaids = new ArrayList<>();

    public RaidManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        DataDrivenManager dataDrivenManager = plugin.getDataDrivenManager();

        for (Raid activeRaid : new ArrayList<>(this.activeRaids)) {
            activeRaid.stopRaidTickLogic();
            activeRaid.stop();
        }
        this.activeRaids.clear();

        List<Raid> newTemplates = new ArrayList<>();

        for (RaidDto raidDto : dataDrivenManager.getRaids()) {
            dataDrivenManager.getArea(raidDto.getAreaIdentifier())
                    .flatMap(AreaDto::buildArea)
                    .ifPresent(area -> {
                        try {
                            var raid = new Raid(raidDto, area, plugin);
                            plugin.getLogger().fine("loaded raid template: " + raid.getIdentifier());
                            newTemplates.add(raid);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    });
        }

        plugin.getLogger().fine("loaded " + newTemplates.size() + " raid templates");
        this.raidTemplates = Collections.unmodifiableList(newTemplates);
    }


    public @NotNull Collection<Raid> getRaidTemplates() {
        return this.raidTemplates;
    }

    public @NotNull Optional<Raid> getRaidTemplateCopy(@NotNull String raidIdentifier) {
        return raidTemplates.stream()
                .filter(raid -> raid.getIdentifier().equalsIgnoreCase(raidIdentifier))
                .findFirst()
                .map(Raid::copy);
    }

    public void registerActiveRaid(@NotNull Raid raid) {
        if (!activeRaids.contains(raid)) {
            activeRaids.add(raid);
        }
    }

    public @NotNull List<ItemStack> getMaps(){
        return raidTemplates
                .stream()
                .map(raid -> raid
                        .getMapRenderer()
                        .create()
                ).toList();
    }

    public void unregisterActiveRaid(@NotNull Raid raid) {
        activeRaids.remove(raid);
    }

    public @NotNull Collection<Raid> getActiveRaids() {
        return Collections.unmodifiableList(this.activeRaids);
    }

    public @NotNull Optional<Raid> getActiveRaid(@NotNull UUID raidId) {
        return activeRaids.stream()
                .filter(raid -> raid.getRaidUniqueIdentifier().equals(raidId))
                .findFirst();
    }

    public @NotNull List<Raid> getActiveRaidsByIdentifier(@NotNull String raidIdentifier) {
        return activeRaids.stream()
                .filter(raid -> raid.getIdentifier().equalsIgnoreCase(raidIdentifier))
                .toList();
    }

    public @NotNull List<Raid> getAllRaidTemplateCopies() {
        return raidTemplates.stream()
                .map(Raid::copy)
                .toList();
    }

    public @NotNull List<Raid> getAvailableRaidTemplateCopies() {
        Set<String> activeIdentifiers = activeRaids.stream()
                .map(Raid::getIdentifier)
                .collect(java.util.stream.Collectors.toSet());

        return raidTemplates.stream()
                .filter(template -> !activeIdentifiers.contains(template.getIdentifier()))
                .map(Raid::copy)
                .toList();
    }
}