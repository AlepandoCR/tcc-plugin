package tcc.gamers.raid;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.event.RaidCompleteEvent;
import tcc.gamers.ai.event.RaidPlayerJoinEvent;
import tcc.gamers.ai.event.RaidPlayerLeaveEvent;
import tcc.gamers.item.dragon.DragonHornHelper;
import tcc.gamers.area.Area;
import tcc.gamers.area.SupervisedArea;
import tcc.gamers.data.RaidDto;
import tcc.gamers.data.RaidMobDto;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Raid extends SupervisedArea {

    private final @NotNull Collection<RaidMob> raidMobs;
    private final @NotNull UUID raidId;
    private final @NotNull BossBar bossBar;
    private boolean isCompleted = false;
    private final @NotNull Location spawnLocation;
    private final @NotNull RaidDto raidDto;
    private final @NotNull Collection<UUID> participants;
    private final @NotNull RaidMapRenderer mapRenderer;

    private RaidLobbyTask lobbyTask;

    public Raid(
            @NotNull RaidDto raidDto,
            @NotNull Area area,
            @NotNull TCCPlugin plugin
    ) throws NoSuchElementException {
        super(area, plugin, raidDto.getTickFrequency());

        var maybeLocation = raidDto.toLocation();
        if (maybeLocation.isEmpty()) {
            throw new NoSuchElementException("World not valid for location on raid dto");
        }

        this.spawnLocation = maybeLocation.get();
        var raidMobsDto = plugin.dataDrivenManager.getRaidMobs();
        var raidMobsStrings = raidDto.getRaidMobs();

        this.raidMobs = new ArrayList<>();
        loadRaidMobs(plugin, raidMobsStrings, raidMobsDto, getAmountsMap(raidDto, raidMobsStrings));

        this.bossBar = buildBossBar();
        this.raidId = UUID.randomUUID();
        this.raidDto = raidDto;
        participants = new ArrayList<>();
        mapRenderer = new RaidMapRenderer(this);
    }

    public @NotNull RaidMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public long getTickFrequency() { return raidDto.getTickFrequency(); }

    public int getRequiredPlayersAmount() {
        return Math.max(1, raidMobs.size() / 2);
    }

    public boolean isCompleted() {
        return this.isCompleted;
    }


    @Override
    protected void onPlayerJoin(@NotNull Player player) {
        if (this.onTick) {
            if(!participants.contains(player.getUniqueId())){
                participants.add(player.getUniqueId());
            }
            bossBar.addViewer(player);
        } else if (!isCompleted) {
            if (lobbyTask == null || lobbyTask.isCancelled()) {
                lobbyTask = new RaidLobbyTask(this, this.plugin);
            }
        }
        new RaidPlayerJoinEvent(this, player).callEvent();
    }

    @Override
    protected void onPlayerLeave(@NotNull Player player) {
        bossBar.removeViewer(player);
        player.sendActionBar(Component.empty());

        if (!this.onTick && getEntitiesInAreaOfType(Player.class).isEmpty()) {
            if (lobbyTask != null && !lobbyTask.isCancelled()) {
                lobbyTask.cancel();
                lobbyTask = null;
            }
        }
        new RaidPlayerLeaveEvent(this, player).callEvent();
    }

    @NotNull
    private @Unmodifiable List<RaidMob> findEscapedMobs() {
        var entitiesInArea = new HashSet<>(getEntitiesInAreaOfType(Entity.class));

        return raidMobs.stream()
                .filter(RaidMob::isAlive)
                .filter(mob -> mob.getBukkitEntity()
                        .map(entity -> !entitiesInArea.contains(entity))
                        .orElse(false) // not spawned
                )
                .toList();
    }

    //tp fugitives
    private void teleportEscapedMobsToSpawn() {
        findEscapedMobs().forEach(mob ->
                mob.getBukkitEntity().ifPresent(entity ->
                        entity.teleport(spawnLocation)
                )
        );
    }

    public void spawnMobs() {
        raidMobs.stream().filter(RaidMob::notSpawned).forEach(mob -> {
            double offsetX = ThreadLocalRandom.current().nextDouble(-2.5, 2.5);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-2.5, 2.5);

            Location spawnLoc = this.spawnLocation.clone().add(offsetX, 0, offsetZ);
            spawnLoc.setY(spawnLoc.getY() + 1.0);
            mob.spawnMob(spawnLoc);
        });
    }

    public void startRaidTickLogic() {
        if (this.getTask().isEmpty()) {
            this.start();
        }

        this.getEntitiesInAreaOfType(Player.class).forEach(player ->{
            if(!participants.contains(player.getUniqueId())){
                participants.add(player.getUniqueId()); // add all player already inside
            }
            bossBar.addViewer(player);
        });
        this.onTick = true;
    }

    public void stopRaidTickLogic() {
        this.onTick = false;
    }

    @Override
    protected void onTick() {
        tickEmotionControllers();
        teleportEscapedMobsToSpawn();

        float targetProgress = getClampedProgress();
        if (Math.abs(this.bossBar.progress() - targetProgress) > 0.01f) {
            this.bossBar.progress(targetProgress);
        }

        if (raidMobs.stream().noneMatch(RaidMob::isAlive)) {
            this.isCompleted = true;
            removeBossBarFromPlayers();
            congratulatePlayers();
            this.stop();
            stopRaidTickLogic();
        }
    }

    @Contract("-> new")
    public @NotNull Raid copy() {
        return new Raid(this.raidDto, this, this.plugin);
    }

    @Override
    protected void onStop() {
        getAliveMobs().forEach(RaidMob::despawnMob);
        removeBossBarFromPlayers();
        if (lobbyTask != null && !lobbyTask.isCancelled()) {
            lobbyTask.cancel();
        }

        plugin.raidManager.unregisterActiveRaid(this);
    }

    @Override
    protected void onStart() {
        bossBar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
        bossBar.addFlag(BossBar.Flag.DARKEN_SCREEN);

        plugin.raidManager.registerActiveRaid(this);
    }

    public @NotNull Collection<RaidMob> getRaidMobs() { return raidMobs; }
    public @NotNull UUID getRaidUniqueIdentifier() { return this.raidId; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    public @NotNull Collection<RaidMob> getAliveMobs() {
        return raidMobs.stream().filter(RaidMob::isAlive).toList();
    }

    private @Range(from = 0, to = 1) float getClampedProgress() {
        int mobAmount = raidMobs.size();
        if (mobAmount == 0) return 0.0f;
        long mobsAlive = raidMobs.stream().filter(RaidMob::isAlive).count();
        float progress = (float) mobsAlive / mobAmount;
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private void tickEmotionControllers() {
        raidMobs.forEach(mob -> mob.getEmotionController().tick());
    }

    private void removeBossBarFromPlayers() {
        for (BossBarViewer viewer : bossBar.viewers()) {
            if (viewer instanceof Player player) {
                bossBar.removeViewer(player);
            }
        }
    }

    private void congratulatePlayers() {

        var dtoMobs = sumList(raidDto.getRaidMobsAmounts());

        var karmaGain = Math.max(dtoMobs / 3, 1);

        int cloudGain  = 0;
        int treeGain = 0;

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);

            if (player == null) continue;

            player.sendMessage(Component.text("Has derrotado la raid!").color(NamedTextColor.GOLD));
            new RaidCompleteEvent(this, player, karmaGain).callEvent();
            player.getInventory().addItem(DragonHornHelper.createSoulShard(karmaGain / 2));

            var lpUser = LuckPermsProvider.get().getUserManager().getUser(uuid);
            if (lpUser == null) continue;

            var groups = lpUser.getInheritedGroups(lpUser.getQueryOptions());
            boolean inNube  = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase("nube"));
            boolean inArbol = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase("arbol"));

            if (inNube) {
                cloudGain += karmaGain;
            } else if (inArbol) {
                treeGain += karmaGain;
            }
        }

        if (cloudGain > 0) {
            var mutable = plugin.configManager.getLiveGameDataManager().getCloudsKarmaMutable();
            mutable.set(mutable.get() + cloudGain);
        }
        if (treeGain > 0) {
            var mutable = plugin.configManager.getLiveGameDataManager().getTreeKarmaMutable();
            mutable.set(mutable.get() + treeGain);
        }
    }

    @Contract(pure = true)
    private int sumList(@NotNull int[] list){
        var sum = 0;
        for (int num : list) {
            sum += num;
        }
        return sum;
    }

    private void loadRaidMobs(@NotNull TCCPlugin plugin, @NotNull List<String> raidMobsStrings, @NotNull Collection<RaidMobDto> raidMobsDto, @NotNull Map<String, Integer> mobsWithAmount) {
        for (String raidMobsString : raidMobsStrings) {
            var maybeRaidMobDto = raidMobsDto.stream().filter(dto -> dto.getRaidMobId().equalsIgnoreCase(raidMobsString)).findFirst();
            if (maybeRaidMobDto.isPresent()) {
                var raidMobDto = maybeRaidMobDto.get();
                var entityAmount = mobsWithAmount.get(raidMobDto.getRaidMobId());
                for (int i = 0; i < entityAmount; i++) {
                    raidMobs.add(new RaidMob(raidMobDto, plugin));
                }
            }
        }
    }

    @NotNull
    private static Map<String, Integer> getAmountsMap(@NotNull RaidDto raidDto, @NotNull List<String> raidMobsStrings) {
        var raidMobsAmounts = raidDto.getRaidMobsAmounts();
        if (raidMobsAmounts.length != raidMobsStrings.size()) {
            throw new InputMismatchException("There should be an amount for each mob. Lists size should match on raid dto.");
        }
        Map<String, Integer> mobsWithAmount = new HashMap<>();
        for (int i = 0; i < raidMobsStrings.size(); i++) {
            mobsWithAmount.put(raidMobsStrings.get(i), raidMobsAmounts[i]);
        }
        return mobsWithAmount;
    }

    @NotNull
    private static BossBar buildBossBar() {
        return BossBar.bossBar(Component.text("Progreso de la raid").color(NamedTextColor.RED), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_12);
    }
}