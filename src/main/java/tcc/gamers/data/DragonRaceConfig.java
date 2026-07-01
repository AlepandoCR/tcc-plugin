package tcc.gamers.data;

import net.democracycraft.democracyLib.api.config.Configurable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Configurable(name = "GeneratedDragonRaceConfig")
public class DragonRaceConfig extends RaceDto {

    public DragonRaceConfig(
            double xCoord,
            double yCoord,
            double zCoord,
            double xRadius,
            double yRadius,
            double zRadius,
            double tickFrequency,
            double checkpointDetectionRadius,
            @NotNull String worldName,
            @NotNull String identifier,
            @NotNull String displayName
    ) {
        super(
                xCoord,
                yCoord,
                zCoord,
                xRadius,
                yRadius,
                zRadius,
                tickFrequency,
                checkpointDetectionRadius,
                worldName,
                identifier,
                displayName
        );
    }

    public DragonRaceConfig(@NotNull GeneratedDragonRaceConfig dragonRaceConfig) {
        this(
                dragonRaceConfig.getXCoord(),
                dragonRaceConfig.getYCoord(),
                dragonRaceConfig.getZCoord(),
                dragonRaceConfig.getXRadius(),
                dragonRaceConfig.getYRadius(),
                dragonRaceConfig.getZRadius(),
                dragonRaceConfig.getTickFrequency(),
                dragonRaceConfig.getCheckpointDetectionRadius(),
                dragonRaceConfig.getWorldName(),
                dragonRaceConfig.getIdentifier(),
                dragonRaceConfig.getDisplayName()
        );
    }

    public void save(File file) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        yamlConfiguration.set("x-coord", this.getX());
        yamlConfiguration.set("y-coord", this.getY());
        yamlConfiguration.set("z-coord", this.getZ());

        yamlConfiguration.set("world", this.getWorldName());

        yamlConfiguration.set("race-identifier", this.getIdentifier());
        yamlConfiguration.setComments("race-identifier", List.of("identifier for the race"));

        this.getDisplayName();
        yamlConfiguration.set("race-display-name", this.getDisplayNameString());
        yamlConfiguration.setComments("race-display-name", List.of("display name for the race"));

        yamlConfiguration.set("x-radius", this.getXRadius());
        yamlConfiguration.setComments("x-radius", List.of("radios for the start location"));

        yamlConfiguration.set("y-radius", this.getYRadius());
        yamlConfiguration.setComments("y-radius", List.of("radios for the start location"));

        yamlConfiguration.set("z-radius", this.getZRadius());
        yamlConfiguration.setComments("z-radius", List.of("radios for the start location"));

        yamlConfiguration.set("tick-frequency", this.getTickFrequency());
        yamlConfiguration.setComments("tick-frequency", List.of("tick frequency (20.0 = 1sg) its a race so less than a second is ideal"));

        yamlConfiguration.set("checkpoint-detection-radius", this.getCheckpointDetectionRadius());
        yamlConfiguration.setComments("checkpoint-detection-radius", List.of("radius for checkpoint detection"));

        try {
            yamlConfiguration.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public CompletableFuture<Void> saveAsync(File file) {
        return CompletableFuture.runAsync(() -> this.save(file));
    }
}