package tcc.gamers.race.dragon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.data.DragonRaceConfig;
import tcc.gamers.nms.nautilus.AbstractDragonNautilus;
import tcc.gamers.nms.nautilus.DragonMountNautilus;
import tcc.gamers.race.Race;
import tcc.gamers.util.path.RacePath;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DragonRace extends Race<DragonRaceConfig> {

    private final @NotNull List<AbstractDragonNautilus<?>> dragons;

    private boolean preparing = false;

    public DragonRace(
            @NotNull TCCPlugin plugin,
            @NotNull RacePath path,
            @NotNull DragonRaceConfig raceDto
    ) {
        super(plugin, path, raceDto);
        dragons = new ArrayList<>();
    }


    @Override
    public void onCheckpointSkip(@NotNull UUID uuid, @NotNull Location lasCheckpointLocation) {
        backToCheckpoint(uuid, lasCheckpointLocation);
    }

    @Override
    public void onParticipantDetach(@NotNull UUID uuid) {
        getDragonTypes(DragonMountNautilus.class).stream()
                .filter(dragon -> dragon.getBukkitOwner().getUniqueId().equals(uuid))
                .findAny()
                .ifPresent(dragonMountNautilus -> {
                    dragons.remove(dragonMountNautilus);
                    dragonMountNautilus.getBukkitNautilus().remove();
                }); // find dragon owned by uuid

        // check if uuid is dragon itself, if so remove it and its mount
        dragons.stream().filter(dragon -> dragon.getUUID().equals(uuid)).findAny().ifPresent(dragon -> {
            dragon.getBukkitNautilus().remove();
            dragons.remove(dragon);
        });
    }

    @Override
    protected void onPlayerCrossCheckpoint(@NotNull UUID entityUuid, @NotNull Location checkpointLocation, int checkpointIndex, boolean isGoal) {
        if(!isGoal){
            getParticipants()
                    .stream()
                    .map(uuid ->
                            plugin
                                    .getServer()
                                    .getEntity(uuid)
                    ).filter(Objects::nonNull)
                    .forEach(audience ->
                            {
                                var player = plugin.getServer().getPlayer(entityUuid);
                                if (player == null) return;
                                audience
                                        .sendMessage(
                                                Component.text("El jugador ").color(NamedTextColor.GRAY)
                                                        .append(
                                                                player.displayName()
                                                                        .color(NamedTextColor.AQUA)
                                                        )
                                                        .append(
                                                                Component.text(" cruzó el checkpoint ")
                                                                        .color(NamedTextColor.GRAY)
                                                        )
                                                        .append(
                                                                Component.text(checkpointIndex + 1)
                                                                        .color(NamedTextColor.YELLOW)
                                                        )
                                        );
                            }
                    );
        }

        spawnCelebrationFirework(
                checkpointLocation,
                List.of(Color.RED, Color.YELLOW, Color.ORANGE),
                List.of(Color.ORANGE, Color.ORANGE),
                Math.min(255, Math.max(1, checkpointIndex))
        );

    }

    public void spawnCelebrationFirework(@NotNull Location location, @NotNull List<Color> mainColors, @NotNull List<Color> fadeColors, int powerLevel) {
        if (location.getWorld() == null) return;

        location.getWorld().spawn(location, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();

            FireworkEffect effect = FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(mainColors)
                    .withFade(fadeColors)
                    .trail(true)
                    .flicker(true)
                    .build();

            meta.addEffect(effect);
            meta.setPower(powerLevel);

            firework.setFireworkMeta(meta);
        });
    }

    @Override
    protected void onPlayerFinishRace(@NotNull UUID entityUuid, int position) {
        getParticipants()
                .stream()
                .map(uuid ->
                        plugin
                                .getServer()
                                .getEntity(uuid)
                ).filter(Objects::nonNull)
                .forEach(audience ->
                        {
                            var player = plugin.getServer().getPlayer(entityUuid);
                            if (player == null) return;
                            audience
                                    .sendMessage(
                                            Component.text("El jugador ").color(NamedTextColor.GRAY)
                                                    .append(
                                                            player.displayName()
                                                            .color(NamedTextColor.AQUA)
                                                    )
                                                    .append(
                                                            Component.text(" termninó la carrera ")
                                                            .color(NamedTextColor.GRAY)
                                                    )
                                                    .append(
                                                            getRaceDto()
                                                                    .getDisplayName()
                                                    ).append(
                                                            Component.text(" en la posición ")
                                                            .color(NamedTextColor.GRAY)
                                                    )
                                                    .append(
                                                            Component.text(position)
                                                                    .color(NamedTextColor.YELLOW)
                                                    )
                                    );
                        }
                );
    }

    @Override
    public void onPlayerOutOfBounds(@NotNull UUID uuid, @NotNull Location playerLocation, @NotNull Location safeLocation) {
        backToCheckpoint(uuid, safeLocation);
    }

    private void backToCheckpoint(@NotNull UUID uuid, @NotNull Location safeLocation) {
        var dragonMountClass = DragonMountNautilus.class;
       var maybeDragon = getDragonTypes(dragonMountClass).stream()
                .filter(dragonMount -> dragonMount.getBukkitOwner().getUniqueId().equals(uuid))
                .findFirst();

        maybeDragon.ifPresent(dragonMount -> {
            var bukkitNautilus = dragonMount.getBukkitNautilus();
            dragonMount.ejectPassengers();
            bukkitNautilus.teleport(safeLocation);
            dragonMount.getBukkitOwner().teleport(safeLocation);
            bukkitNautilus.addPassenger(dragonMount.getBukkitOwner());
        });
    }

    @Override
    protected void onPrepareRace() {
        preparing = true;

        generateStartGridAsync().thenAccept(points -> {
            points.forEach((uuid, location) -> {
                var player = plugin.getServer().getPlayer(uuid);
                if (player == null) return;

                AbstractDragonNautilus<?> dragon = new DragonMountNautilus(location.getWorld(), plugin, player);
                dragon.spawn(location);
                dragon.getBukkitNautilus().addPassenger(player);
                dragon.setFreeze(true);
                dragons.add(dragon);
            });
        });
    }

    @Override
    protected void onRaceStart() {
        preparing = false;
        dragons.forEach(dragon -> dragon.setFreeze(false));
    }

    @Override
    public void onStop() {
        super.onStop();
        preparing = false;
        dragons.forEach(dragon -> dragon.getBukkitNautilus().remove());
        plugin.getRaceManager().unregisterRace(this.identifier);
    }

    @Override
    protected void onStart() {
        //noting here
    }

    @NotNull
    public List<AbstractDragonNautilus<?>> getDragons() {
        return dragons;
    }

    public <DragonType extends AbstractDragonNautilus<?>> @NotNull List<DragonType> getDragonTypes(@NotNull Class<DragonType> dragonTypeClass) {
        return dragons
                .stream()
                .filter(dragonTypeClass::isInstance)
                .map(dragonTypeClass::cast)
                .toList();
    }

    public boolean isPreparing() {
        return preparing;
    }

    /**
     * Asynchronously calculates the 2D spatial grid for participants and synchronously resolves
     * the highest Y block on the main thread.
     *
     * @return CompletableFuture containing a map linking each participant's UUID to their start location.
     */
    public CompletableFuture<Map<UUID, Location>> generateStartGridAsync() {
        List<UUID> participantList = new ArrayList<>(getParticipants());
        int count = participantList.size();

        if (count == 0) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        Location firstCheckpoint = getPath().getCheckpointLocation(0);

        if (firstCheckpoint == null) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        // Base forward direction pointing to the first checkpoint
        Vector direction = firstCheckpoint.toVector().subtract(center.toVector());
        direction.setY(0).normalize();

        Location baseFacing = center.clone().setDirection(direction);
        float sharedYaw = baseFacing.getYaw();
        float sharedPitch = baseFacing.getPitch();

        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, double[]> calculatedOffsets = new HashMap<>(count);
            double spacing = 0.8;

            int cols = (int) Math.ceil(Math.sqrt(count));
            int rows = (int) Math.ceil((double) count / cols);

            // Perpendicular vectors to build the grid based on the facing direction
            Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            Vector back = direction.clone().multiply(-1);

            for (int i = 0; i < count; i++) {
                int row = i / cols;
                int col = i % cols;

                // Center grid around the start location
                double offsetX = (col - (cols - 1) / 2.0) * spacing;
                double offsetZ = (row - (rows - 1) / 2.0) * spacing;

                double worldX = center.getX() + (right.getX() * offsetX) + (back.getX() * offsetZ);
                double worldZ = center.getZ() + (right.getZ() * offsetX) + (back.getZ() * offsetZ);

                calculatedOffsets.put(participantList.get(i), new double[]{worldX, worldZ});
            }

            return calculatedOffsets;

        }).thenApplyAsync(pointsMap -> {
            // Resolving block heights must run on the main server thread
            World world = center.getWorld();
            Map<UUID, Location> gridLocations = new HashMap<>(pointsMap.size());

            for (Map.Entry<UUID, double[]> entry : pointsMap.entrySet()) {
                double[] pt = entry.getValue();
                double x = pt[0];
                double z = pt[1];

                // +1 to spawn on top of the block, not inside it
                int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;

                gridLocations.put(entry.getKey(), new Location(world, x, y, z, sharedYaw, sharedPitch));
            }

            return gridLocations;

        }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }
}