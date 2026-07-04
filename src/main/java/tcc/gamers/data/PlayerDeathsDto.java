package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.ConfigValue;
import net.democracycraft.democracyLib.api.config.Configurable;
import org.jetbrains.annotations.NotNull;

@Configurable(name = "GeneratedPlayerDeathsDto")
public class PlayerDeathsDto {

    @ConfigValue(fieldName = "player-unique-identifier")
    private final String playerUniqueIdentifier;

    @ConfigValue(fieldName = "deaths")
    private int deaths;

    @ConfigValue(fieldName = "lives")
    private int lives;

    public PlayerDeathsDto(@NotNull String playerUniqueIdentifier) {
        this.playerUniqueIdentifier = playerUniqueIdentifier;
        this.deaths = 0;
        this.lives = 3;
    }

    public PlayerDeathsDto(@NotNull GeneratedPlayerDeathsDto generatedPlayerDeathsDto) {
        this.playerUniqueIdentifier = generatedPlayerDeathsDto.getPlayerUniqueIdentifier();
        this.deaths = generatedPlayerDeathsDto.getDeaths();
        this.lives = generatedPlayerDeathsDto.getLives();
    }

    public @NotNull String getPlayerUniqueIdentifier() {
        return playerUniqueIdentifier;
    }

    public void addDeath(){
        deaths++;
    }

    public void removeDeath(){
        deaths--;
    }

    public boolean isPermaDeath(){
        return deaths >= lives;
    }

    public void addLive(){
        this.lives++;
    }

    public void removeLive(){
        this.lives--;
    }

    public int getLives() {
        return lives;
    }

    public int getDeaths() {
        return deaths;
    }
}
