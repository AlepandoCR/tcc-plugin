package tcc.gamers.race;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.area.SupervisedArea;
import tcc.gamers.data.RaceDto;
import tcc.gamers.event.race.EntityCrossCheckpointEvent;
import tcc.gamers.event.race.EntityFinishRaceEvent;
import tcc.gamers.tutorials.model.Model;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;
import tcc.gamers.util.path.RacePath;

import java.time.Duration;
import java.util.*;

public abstract class Race<RaceDtoType extends RaceDto> extends SupervisedArea {
    private final @NotNull RacePath path;
    private final @NotNull RaceDtoType raceDto;
    private final @NotNull List<UUID> participants;
    private final @NotNull BossBar bossBar;

    private final @NotNull Set<UUID> finishedParticipants;
    private final @NotNull Map<UUID, Integer> finishPositions;

    private volatile @NotNull List<ParticipantDto> rankedRecords;
    private @NotNull List<UUID> lastTopParticipants;

    private @Nullable RaceLobbyTask lobbyTask = null;
    private @Nullable Long countdownEndTime = null;
    private final @NotNull List<Entity> spawnedCheckpoints = new ArrayList<>();

    protected Race(
            @NotNull TCCPlugin plugin,
            @NotNull RacePath path,
            @NotNull RaceDtoType raceDto
    ) {
        super(
                raceDto.toLocation().orElseThrow(),
                raceDto.getIdentifier(),
                plugin,
                raceDto.getXRadius(),
                raceDto.getYRadius(),
                raceDto.getZRadius(),
                (long) raceDto.getTickFrequency()
        );
        this.path = path;
        this.raceDto = raceDto;
        this.participants = new ArrayList<>();
        this.finishedParticipants = new HashSet<>();
        this.finishPositions = new HashMap<>();
        this.rankedRecords = Collections.emptyList();
        this.lastTopParticipants = Collections.emptyList();
        this.bossBar = buildBossBar();
    }

    public void startRace() {
        plugin.getLogger().info("[Race-Log] Race started. Participants count: " + participants.size());
        calculatePositions();
        this.countdownEndTime = null;
        this.onTick = true;
        onRaceStart();
    }

    @Override
    protected void onPlayerJoin(@NotNull Player player) {
        if (!this.onTick) { // if preparing add participants
            if (!participants.contains(player.getUniqueId())) {
                addParticipant(player.getUniqueId());
            } else {
                bossBar.addViewer(player);
            }

            if (lobbyTask == null || lobbyTask.isCancelled()) {
                lobbyTask = new RaceLobbyTask(this, this.plugin);

            }
        }
    }

    @Override
    protected void onPlayerLeave(@NotNull Player player) {
        if(!this.onTick){
            removeParticipant(player.getUniqueId());
            if (getEntitiesInAreaOfType(Player.class).isEmpty()) {
                if (lobbyTask != null && !lobbyTask.isCancelled()) {
                    lobbyTask.cancel();
                    lobbyTask = null;
                }
            }
        }
    }

    @Override
    public void onTick() {
        calculatePositions();
        checkRaceState();
    }

    private void checkRaceState() {
        long activePlayers = participants.stream()
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .count();

        if (activePlayers == 0 || finishedParticipants.size() >= activePlayers) {
            plugin.getLogger().warning("[Race-Log] Stopping race! Condition met: activePlayers (" + activePlayers + ") == 0 OR finished (" + finishedParticipants.size() + ") >= active (" + activePlayers + ")");
            this.stop();
            this.start();
            return;
        }

        // Stop if everyone left or if all active participants have finished
        if (participants.isEmpty() || finishedParticipants.size() >= participants.size()) {
            plugin.getLogger().warning("[Race-Log] Stopping race! Condition met: participants empty OR finished >= total participants.");
            this.stop();
            this.start();
            return;
        }

        if (countdownEndTime == null) {
            int requiredFinished = (int) Math.ceil(participants.size() / 2.0);
            if (finishedParticipants.size() >= requiredFinished) {
                plugin.getLogger().info("[Race-Log] Countdown started. Finished: " + finishedParticipants.size() + " >= Required: " + requiredFinished);
                startEndCountdown();
            }
        } else if (System.currentTimeMillis() >= countdownEndTime) {
            plugin.getLogger().info("[Race-Log] Countdown finished. Stopping race.");
            this.stop();
            this.start();
        }
    }

    private void teleportToStart() {
        finishedParticipants.forEach(uuid ->{
            var entity = plugin.getServer().getEntity(uuid);
            if(entity instanceof Player player){
                player.teleport(center);
            }
        });
    }

    private void startEndCountdown() {
        this.countdownEndTime = System.currentTimeMillis() + 60_000L; // 60 seconds
    }

    private void calculatePositions() {
        if (participants.isEmpty()) {
            this.rankedRecords = Collections.emptyList();
            updateBossBarIfNeeded(Collections.emptyList());
            return;
        }

        List<ParticipantDto> previousRecords = this.rankedRecords;

        Map<UUID, ParticipantDto> previousByUuid = new HashMap<>(previousRecords.size());
        for (ParticipantDto record : previousRecords) {
            previousByUuid.put(record.uuid(), record);
        }

        List<ParticipantDto> records = new ArrayList<>(participants.size());
        Iterator<UUID> iterator = participants.iterator();

        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            var entity = plugin.getServer().getEntity(uuid);

            if (entity == null || !entity.isValid()) {
                plugin.getLogger().warning("[Race-Log] Removing participant " + uuid + " because entity is null or invalid.");
                iterator.remove();
                detachParticipant(uuid);
                continue;
            }

            if (!(entity instanceof Player)) {
                iterator.remove();
                continue;
            }

            Location loc = entity.getLocation();
            int closestIndex = path.getClosestCheckpointIndex(loc);
            if (closestIndex == -1) {
                continue;
            }

            double closestDistance = path.getDistanceToCheckpoint(loc, closestIndex);

            ParticipantDto previousRecord = previousByUuid.get(uuid);
            List<Integer> previousCrossed = previousRecord != null
                    ? previousRecord.crossedCheckpoints()
                    : List.of();

            List<Integer> crossed = checkCheckpointCrossing(
                    uuid, entity, previousCrossed, closestIndex, closestDistance
            );

            checkOutOfBounds(uuid, loc, crossed);

            double rankDistance = computeRankDistance(loc, crossed, closestDistance);
            records.add(new ParticipantDto(uuid, crossed, rankDistance));
        }

        records.sort((a, b) -> {
            Integer posA = finishPositions.get(a.uuid());
            Integer posB = finishPositions.get(b.uuid());

            if (posA != null || posB != null) {
                if (posA == null) return 1;
                if (posB == null) return -1;
                return Integer.compare(posA, posB);
            }

            int countCompare = Integer.compare(b.crossedCheckpoints().size(), a.crossedCheckpoints().size());
            if (countCompare != 0) {
                return countCompare;
            }
            return Double.compare(a.distance(), b.distance());
        });

        this.rankedRecords = List.copyOf(records);

        List<UUID> currentTop = new ArrayList<>();
        for (int i = 0; i < Math.min(5, records.size()); i++) {
            currentTop.add(records.get(i).uuid());
        }

        updateBossBarIfNeeded(currentTop);
    }

    private double computeRankDistance(
            @NotNull Location loc,
            @NotNull List<Integer> crossed,
            double fallbackDistance
    ) {
        int lastConfirmed = crossed.isEmpty() ? -1 : crossed.getLast();

        if (lastConfirmed >= 0 && path.isLastCheckpoint(lastConfirmed)) {
            return 0;
        }

        int targetIndex = lastConfirmed + 1;
        Location targetLoc = path.getCheckpointLocation(targetIndex);
        if (targetLoc == null) {
            return fallbackDistance;
        }

        return path.getDistanceToCheckpoint(loc, targetIndex);
    }

    private @NotNull List<Integer> checkCheckpointCrossing(
            @NotNull UUID uuid,
            @NotNull org.bukkit.entity.Entity entity,
            @NotNull List<Integer> previousCrossed,
            int closestIndex,
            double closestDistance
    ) {
        if (closestDistance > raceDto.getCheckpointDetectionRadius()) {
            return previousCrossed;
        }

        int lastConfirmed = previousCrossed.isEmpty() ? -1 : previousCrossed.getLast();

        if (closestIndex == lastConfirmed + 1) {
            plugin.getLogger().info("[Race-Log] Player " + entity.getName() + " crossing checkpoint " + closestIndex);
            List<Integer> updated = new ArrayList<>(previousCrossed);
            updated.add(closestIndex);

            Location checkpointLocation = path.getCheckpointLocation(closestIndex);
            if (checkpointLocation != null) {
                boolean isGoal = path.isLastCheckpoint(closestIndex);
                plugin.getLogger().info("[Race-Log] Checkpoint " + closestIndex + " isGoal? " + isGoal);

                notifyCheckpointCrossed(uuid, checkpointLocation, closestIndex, isGoal);

                if (isGoal && finishedParticipants.add(uuid)) {
                    plugin.getLogger().info("[Race-Log] Player " + entity.getName() + " added to finishedParticipants.");
                    onParticipantFinish(uuid, checkpointLocation, closestIndex);
                }
            }

            return List.copyOf(updated);
        }

        if (closestIndex > lastConfirmed + 1) {
            plugin.getLogger().warning("[Race-Log] Player " + entity.getName() + " skipped checkpoint! Expected: " + (lastConfirmed + 1) + ", Got: " + closestIndex);
            if( entity instanceof Player player){
                var title = Title.title(
                        Component.text("¡Checkpoint perdido!").color(NamedTextColor.RED),
                        Component.text("Continua el circuito en orden").color(NamedTextColor.YELLOW),
                        Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofSeconds(2),
                                Duration.ofMillis(500)
                        )
                );

                player.showTitle(title);
            }

            Location lastCheckpointLocation = lastConfirmed >= 0
                    ? path.getCheckpointLocation(lastConfirmed)
                    : center;

            if (lastCheckpointLocation != null) {
                onCheckpointSkip(entity.getUniqueId(),lastCheckpointLocation);
            }
        }

        return previousCrossed;
    }

    private void onParticipantFinish(@NotNull UUID uuid, @NotNull Location goalLocation, int index) {
        int position = finishPositions.size() + 1;
        finishPositions.put(uuid, position);
        new EntityFinishRaceEvent(this, uuid, goalLocation, index, position).callEvent();
        onPlayerFinishRace(uuid, position);
    }

    public abstract void onCheckpointSkip(@NotNull UUID uuid, @NotNull Location lasCheckpointLocation);

    private void detachParticipant(@NotNull UUID uuid) {
        var player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            bossBar.removeViewer(player);
        }

        onParticipantDetach(uuid);
    }

    public abstract void onParticipantDetach(@NotNull UUID uuid);

    private void notifyCheckpointCrossed(
            @NotNull UUID uuid,
            @NotNull Location checkpointLocation,
            int checkpointIndex,
            boolean isGoal
    ) {

        new EntityCrossCheckpointEvent(this, uuid, checkpointLocation, checkpointIndex, isGoal).callEvent();
        onPlayerCrossCheckpoint(uuid, checkpointLocation, checkpointIndex, isGoal);
    }


    protected abstract void onPlayerCrossCheckpoint(
            @NotNull UUID entityUuid,
            @NotNull Location checkpointLocation,
            int checkpointIndex,
            boolean isGoal
    );

    protected abstract void onPlayerFinishRace(
            @NotNull UUID entityUuid,
            int position
    );

    private void updateBossBarIfNeeded(@NotNull List<UUID> currentTop) {
        if (currentTop.equals(lastTopParticipants)) {
            return;
        }

        this.lastTopParticipants = currentTop;

        if (currentTop.isEmpty()) {
            bossBar.name(Component.text("Clasificación"));
            return;
        }

        Component newName = Component.empty();
        for (int i = 0; i < currentTop.size(); i++) {
            UUID uuid = currentTop.get(i);
            newName = newName.append(Component.text((i + 1) + " - "));

            Optional<Component> faceOpt = getPlayerFaceComponent(uuid);
            if (faceOpt.isPresent()) {
                newName = newName.append(faceOpt.get());
            } else {
                newName = newName.append(Component.text("?"));
            }

            newName = newName.append(Component.space());
        }

        bossBar.name(newName);
    }

    private void checkOutOfBounds(@NotNull UUID uuid, @NotNull Location playerLocation, @NotNull List<Integer> crossedCheckpoints) {
        int lastConfirmed = crossedCheckpoints.isEmpty() ? -1 : crossedCheckpoints.getLast();
        int targetIndex = lastConfirmed + 1;

        Location targetLoc = path.getCheckpointLocation(targetIndex);
        if (targetLoc == null) {
            return;
        }

        Location lastLoc = lastConfirmed >= 0 ? path.getCheckpointLocation(lastConfirmed) : center;
        if (lastLoc == null) {
            return;
        }

        if (!Objects.equals(lastLoc.getWorld(), targetLoc.getWorld()) || !Objects.equals(playerLocation.getWorld(), targetLoc.getWorld())) {
            return;
        }

        double baseDistance = lastLoc.distance(targetLoc);
        double maxAllowedDistance = baseDistance * 1.5;


        double maxAllowedDistanceSquared = maxAllowedDistance * maxAllowedDistance;

        if (playerLocation.distanceSquared(targetLoc) > maxAllowedDistanceSquared) {
            plugin.getLogger().warning("[Race-Log] Player OUT OF BOUNDS! Current Distance Sq: " + playerLocation.distanceSquared(targetLoc) + " > Max Allowed Sq: " + maxAllowedDistanceSquared);
            onPlayerOutOfBounds(uuid, playerLocation, lastLoc);
        }
    }

    public abstract void onPlayerOutOfBounds(@NotNull UUID uuid, @NotNull Location playerLocation, @NotNull Location safeLocation);

    protected abstract void onPrepareRace();

    public void prepareRace(){
        spawnCheckPoints();
        onPrepareRace();
    }

    private void spawnCheckPoints(){
        var points = path.getLocationList();
        var goal = points.getLast();

        var checkpoints = points.stream().filter(loc -> !loc.equals(goal)).toList();

        Location lastCheckpoint = center;

        for (Location checkpoint : checkpoints) {
            spawnCheckpoint(checkpoint, lastCheckpoint, "tcc_race_checkpoint");

            lastCheckpoint = checkpoint;
        }

        spawnCheckpoint(goal, lastCheckpoint,"tcc_race_goal");
    }
    private void spawnCheckpoint(@NotNull Location checkpoint, @NotNull Location lastCheckpoint, @NotNull String modelName) {
        var direction = lastCheckpoint.toVector().subtract(checkpoint.toVector());
        var lookAtLocation = checkpoint.clone().setDirection(direction);

        float yaw = lookAtLocation.getYaw();
        float pitch = lookAtLocation.getPitch();

        var spawn = checkpoint.clone().addRotation(yaw,pitch);

        var model = Model.spawn(modelName, spawn, BlockDisplay.class);
        var entity = model.getEntity();

        entity.getPersistentDataContainer().set(
                TCCNameSpacedKeys.RACE_CHECKPOINT.getNamespacedKey(),
                PersistentDataType.STRING,
                identifier
        );

        var scale = Math.min(16.0, raceDto.getCheckpointDetectionRadius());
        model.scale(scale);

        Transformation transform = entity.getTransformation();

        transform.getLeftRotation().rotationYXZ(
                (float) Math.toRadians(-yaw),
                (float) Math.toRadians(pitch),
                0.0f
        );

        entity.setTransformation(transform);
        model.setGlowing(16764133);

        this.spawnedCheckpoints.add(entity);
    }

    @Override
    public void onStop(){
        plugin.getLogger().info("[Race-Log] onStop() called. Cleaning up race.");
        removeCheckpoints();
        teleportToStart();
        this.onTick = false;
        this.rankedRecords = Collections.emptyList();
        this.lastTopParticipants = Collections.emptyList();
        this.finishedParticipants.clear();
        this.lobbyTask = null;
        this.finishPositions.clear();
        this.countdownEndTime = null;
        for (UUID participant : participants) {
            detachParticipant(participant);
        }
        this.participants.clear();
    }

    private void removeCheckpoints() {
        for (Entity entity : this.spawnedCheckpoints) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        this.spawnedCheckpoints.clear();
    }

    protected abstract void onRaceStart();

    public void addParticipant(@NotNull UUID participant) {
        this.participants.add(participant);
        var player = plugin.getServer().getPlayer(participant);
        if (player != null) {
            bossBar.addViewer(player);
        }
    }

    public void removeParticipant(@NotNull UUID participant) {
        plugin.getLogger().info("[Race-Log] removeParticipant called for: " + participant);
        this.participants.remove(participant);
        detachParticipant(participant);
    }

    public boolean isParticipant(@NotNull UUID participant) {
        return this.participants.contains(participant);
    }

    public boolean hasFinished(@NotNull UUID participant) {
        return this.finishedParticipants.contains(participant);
    }

    public @NotNull Optional<Integer> getFinishPosition(@NotNull UUID participant) {
        return Optional.ofNullable(finishPositions.get(participant));
    }

    public @NotNull Optional<Integer> getParticipantPosition(@NotNull UUID participant) {
        List<ParticipantDto> currentRecords = this.rankedRecords;
        for (int i = 0; i < currentRecords.size(); i++) {
            if (currentRecords.get(i).uuid().equals(participant)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }

    private @NotNull BossBar buildBossBar() {
        return BossBar.bossBar(Component.text("Clasificación"), 0, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    private @NotNull Optional<Component> getPlayerFaceComponent(@NotNull UUID uuid) {
        var player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }

        var headObject = ObjectContents
                .playerHead()
                .id(uuid)
                .hat(true)
                .build();

        return Optional.of(Component.object(headObject));
    }

    @NotNull
    public Collection<UUID> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    @NotNull
    public RacePath getPath() {
        return path;
    }

    @NotNull
    public Location getStartLocation() { // wrap for clarity
        return center;
    }

    @NotNull
    public RaceDtoType getRaceDto() {
        return raceDto;
    }
}