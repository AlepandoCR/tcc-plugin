package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

@Configurable(name = "GeneratedRaidMobDto")
public class RaidMobDto {

    @ConfigValue(fieldName = "mythic-mobs-id")
    private final String mobId;

    @ConfigValue(fieldName = "mob-level")
    private final int mobLevel;

    @ConfigValue(fieldName = "raid-mob-id", comment = "a better id than mythic mobs naming c:")
    private final String raidMobId;

    public RaidMobDto(@NotNull String mobId, @NotNull String raidMobId, int mobLevel) {
        this.mobId = mobId;
        this.mobLevel = mobLevel;
        this.raidMobId = raidMobId;
    }

    public RaidMobDto(@NotNull GeneratedRaidMobDto generatedRaidMobDto){
        this(
                generatedRaidMobDto.getMobId(),
                generatedRaidMobDto.getRaidMobId(),
                generatedRaidMobDto.getMobLevel()

        );
    }

    public @NotNull String getMobId() {
        return mobId;
    }

    public int getMobLevel() {
        return mobLevel;
    }

    public @NotNull String getRaidMobId(){
        return this.raidMobId;
    }
}
