package tcc.gamers.nms.horses;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spartan.api.engine.config.CuriosityDrivenRecurrentSoftActorCriticConfig;
import org.spartan.api.engine.config.SpartanModelConfig;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.path.Path;
import tcc.gamers.util.path.PathMode;

/**
 * Centralized taxi spawner to avoid code duplication
 * Handles creation, spawning, mounting, and naming of taxi horses
 */
public class TaxiSpawner {

    /**
     * Spawns a taxi at the given location for the player
     * @param plugin Plugin instance
     * @param path Path for the taxi to follow
     * @param pathMode Direction to traverse the path
     * @param location Location to spawn the taxi
     * @param player Player to mount on the taxi
     * @return The spawned CustomSpartanHorse, or null if spawn failed
     */
    @SuppressWarnings("unused")
    public static @Nullable CustomSpartanHorse<?> spawnTaxi(
            @NotNull TCCPlugin plugin,
            @NotNull Path path,
            @NotNull PathMode pathMode,
            @NotNull Location location,
            @NotNull Player player
    ) {
        try {
            CraftWorld craftWorld = (CraftWorld) location.getWorld();
            if (craftWorld == null) {
                player.sendMessage(Component.text("Failed to spawn taxi: Invalid world", NamedTextColor.RED));
                return null;
            }

            SpartanModelConfig modelConfig = buildSpeedModelConfig();
            CustomSpartanHorse<?> horse = new CustomSpartanHorse<>(
                    craftWorld.getHandle(),
                    path,
                    pathMode,
                    plugin.getHorseConfig(),
                    modelConfig,
                    plugin
            );

            horse.spawn(location);
            mountAndName(horse, player, plugin);

            return horse;
        } catch (Exception ex) {
            player.sendMessage(Component.text("Failed to spawn taxi: " + ex.getMessage(), NamedTextColor.RED));
            plugin.getLogger().warning("Error spawning taxi: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Spawns a taxi using the nearest path endpoint as the starting reference.
     * This is used by the taxi menu to automatically choose start-to-finish or finish-to-start.
     */
    @SuppressWarnings("unused")
    public static CustomSpartanHorse<?> spawnTaxi(
            @NotNull TCCPlugin plugin,
            @NotNull Path path,
            @NotNull Location location,
            @NotNull Player player
    ) {
        PathMode autoMode = selectAutoMode(path, player);
        return spawnTaxi(plugin, path, autoMode, location, player);
    }

    private static @NotNull PathMode selectAutoMode(@NotNull Path path, @NotNull Player player) {
        var locations = path.getLocationList();
        if (locations.isEmpty()) {
            return PathMode.CLOSEST_TO_FURTHEST;
        }

        var playerLocation = player.getLocation();
        var first = locations.getFirst();
        var last = locations.getLast();

        double firstDistance = first.distanceSquared(playerLocation);
        double lastDistance = last.distanceSquared(playerLocation);

        // If the player is closer to the final point, drive the route backwards.
        return firstDistance <= lastDistance ? PathMode.CLOSEST_TO_FURTHEST : PathMode.FURTHEST_TO_CLOSEST;
    }

    /**
     * Mounts a player on a taxi and sets its custom name
     */
    private static void mountAndName(
            @NotNull CustomSpartanHorse<?> horse,
            @NotNull Player player,
            @NotNull TCCPlugin plugin
    ) {
        try {
            var bukkitHorse = horse.getBukkitEntity();
            bukkitHorse.addPassenger(player);

            // Set custom name from MiniMessage template
            try {
                String template = plugin.getHorseConfig().getNameMiniMessage();
                MiniMessage mm = MiniMessage.miniMessage();
                Component component = mm.deserialize(template.replace("%player%", player.getName()));
                bukkitHorse.customName(component);
                bukkitHorse.setCustomNameVisible(true);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to set taxi custom name: " + ex.getMessage());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to mount player on taxi: " + ex.getMessage());
        }
    }

    /**
     * Builds the Spartan model config for taxi horses
     */
    @Contract(" -> new")
    private static @NotNull SpartanModelConfig buildSpeedModelConfig() {
        return CuriosityDrivenRecurrentSoftActorCriticConfig.builder()
                // Network architecture
                .actorHiddenLayerCount(1)
                .actorHiddenLayerNeuronCount(32)
                .criticHiddenLayerCount(1)
                .criticHiddenLayerNeuronCount(32)

                // Recurrence - distance is Markovian, kept minimal
                .recurrentLayerDepth(1)
                .hiddenStateSize(16)
                .recurrentInputFeatureCount(16)
                // TBPTT depth matches recurrent depth, no need to unroll further
                .truncatedBPTTDepth(1)

                // SAC core
                .learningRate(3e-4)
                .firstCriticLearningRate(3e-4)
                .secondCriticLearningRate(3e-4)
                .policyNetworkLearningRate(3e-4)
                .gamma(0.95)                        // short horizon, immediate distance reward dominates
                .targetSmoothingCoefficient(0.005)

                // Entropy - 1 continuous action, target entropy = -dim(A) = -1
                .targetEntropy(-1.0)
                .entropyTemperatureAlpha(0.05)      // low initial alpha, action space is trivially simple
                .alphaLearningRate(3e-4)            // standard LR for alpha adaptation

                // tanh squashing enabled - keeps velocity output bounded in [-1, 1]
                .squashActionsWithTanh(1.0)

                // 1D state space, small buffer is sufficient
                .remorseTraceBufferCapacity(200)
                .remorseMinimumSimilarityThreshold(0.7)

                // Forward dynamics - predicts (distance, velocity) -> distance', trivially simple
                .forwardDynamicsHiddenLayerDimensionSize(16)
                .forwardDynamicsLearningRate(3e-4)

                // Inverse dynamics - infers action from (state, next_state), equally simple
                .inverseDynamicsHiddenLayerDimensionSize(16)
                .inverseDynamicsLearningRate(3e-4)
                .inverseLossWeight(0.05)            // regularizer only, not the main signal

                // Dense extrinsic reward (-distance) already guides learning well
                // Curiosity scaled to near-zero to avoid interference
                .intrinsicRewardScale(0.001)
                .intrinsicRewardClampingMinimum(-0.1)
                .intrinsicRewardClampingMaximum(0.1)

                .isTraining(true)
                .build();
    }
}




