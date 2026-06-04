package tcc.gamers.data;


import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * even tho {@link tcc.gamers.raid.Raid} is a {@link tcc.gamers.area.SupervisedArea}, we still use a separate config
 * so we do not mix coords and radius configuration with difficulty and mobs selection
 */
@Configurable(name = "GeneratedRaidDto")
public class RaidDto extends LocationDto{

    @ConfigValue(fieldName = "area-identifier", comment = "links with the area data file")
    private final String areaIdentifier;

    @ConfigValue(fieldName = "tick-frequency")
    private final long tickFrequency;

    @ConfigValue(fieldName = "raid-mobs-ids", comment = "ids defined on raid mobs data files")
    private final List<String> raidMobs;

    @ConfigValue(fieldName = "raid-mobs-amounts", comment = "defines the amount of mobs, related to this same index, must be the same size as raid-mobs-ids")
    private final List<String> raidMobsAmounts;

    public RaidDto(
            @NotNull String areaIdentifier,
            @NotNull List<String> raidMobs,
            long tickFrequency,
            double xCoord,
            double yCoord,
            double zCoord,
            @NotNull String worldName,
            @NotNull List<String> raidMobsAmounts
    ) {
        super(
                xCoord,
                yCoord,
                zCoord,
                worldName
        );
        this.areaIdentifier = areaIdentifier;
        this.raidMobs = raidMobs;
        this.tickFrequency = tickFrequency;
        this.raidMobsAmounts = raidMobsAmounts;
    }

    public RaidDto(@NotNull GeneratedRaidDto generatedRaidDto) {
        this(
                generatedRaidDto.getAreaIdentifier(),
                generatedRaidDto.getRaidMobs(),
                generatedRaidDto.getTickFrequency(),
                generatedRaidDto.getXCoord(),
                generatedRaidDto.getYCoord(),
                generatedRaidDto.getZCoord(),
                generatedRaidDto.getWorldName(),
                generatedRaidDto.getRaidMobsAmounts()
        );
    }

    public String getAreaIdentifier() {
        return areaIdentifier;
    }

    public List<String> getRaidMobs() {
        return raidMobs;
    }

    public long getTickFrequency() {
        return tickFrequency;
    }

    public int[] getRaidMobsAmounts() {

        try{
            var integerList = raidMobsAmounts.stream().map(Integer::valueOf).toList();
            var raidMobsAmounts = new int[integerList.size()];
            for (int i = 0; i < integerList.size(); i++) {
                raidMobsAmounts[i] = integerList.get(i);
            }

            return raidMobsAmounts;
        }catch (NumberFormatException ignored){
        }

        return new int[]{0};
    }
}
