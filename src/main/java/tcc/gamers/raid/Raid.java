package tcc.gamers.raid;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import tcc.gamers.TCCPlugin;
import tcc.gamers.area.Area;
import tcc.gamers.area.SupervisedArea;
import tcc.gamers.data.RaidDto;
import tcc.gamers.data.RaidMobDto;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Raid extends SupervisedArea { // raid IS a supervised area

    private final @NotNull Collection<RaidMob> raidMobs;

    private final @NotNull UUID raidId;

    private final @NotNull BossBar bossBar;

    private boolean isCompleted = false;

    private final @NotNull Location spawnLocation;

    public Raid(
            @NotNull RaidDto raidDto,
            @NotNull Area area,
            @NotNull TCCPlugin plugin
    ) throws NoSuchElementException {
        super(
                area,
                plugin,
                raidDto.getTickFrequency()
        );

        var maybeLocation = raidDto.toLocation();

        if(maybeLocation.isEmpty()){
            throw new NoSuchElementException("World not valid for location on raid dto");
        }

        this.spawnLocation = maybeLocation.get();
        var raidMobsDto = plugin.dataDrivenManager.getRaidMobs();
        var raidMobsStrings = raidDto.getRaidMobs();

        this.raidMobs = new ArrayList<>();

        loadRaidMobs(
                plugin,
                raidMobsStrings,
                raidMobsDto,
                getAmountsMap(
                        raidDto,
                        raidMobsStrings
                )
        );

        this.bossBar = buildBossBar();
        this.raidId = UUID.randomUUID();
    }

    public void spawnMobs() {
        raidMobs.stream().filter(RaidMob::notSpawned).forEach(mob -> {
            double offsetX = ThreadLocalRandom.current().nextDouble(-2.5, 2.5);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-2.5, 2.5);

            Location spawnLoc = this.spawnLocation.clone().add(offsetX, 0, offsetZ);

            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1.0);

            mob.spawnMob(spawnLoc);
        });
    }

    public void startRaidTickLogic() {
        if (this.getTask().isEmpty()) {
            this.start();
        }

        if (!this.onTick) {
            showBossBarToPlayers();
        }

        this.onTick = true;
    }


    public void stopRaidTickLogic(){
        this.onTick = false;
    }

    @Override
    protected void onTick() {
        checkForCompletion();

        tickEmotionControllers();

        float targetProgress = getClampedProgress();

        if (Math.abs(this.bossBar.progress() - targetProgress) > 0.01f) {
            this.bossBar.progress(targetProgress);
        }

        showBossBarToPlayers();

        if (raidMobs.stream().noneMatch(RaidMob::isAlive)) {
            this.isCompleted = true;

            removeBossBarToPlayers();
            stopRaidTickLogic();
        }
    }

    @Override
    protected void onStop() {
        getAliveMobs().forEach(RaidMob::despawnMob);
        removeBossBarToPlayers();
    }

    @Override
    protected void onStart() {
        showBossBarToPlayers();

        bossBar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
        bossBar.addFlag(BossBar.Flag.DARKEN_SCREEN);
    }

    public @NotNull Collection<RaidMob> getRaidMobs(){
        return raidMobs;
    }

    public @NotNull UUID getRaidUniqueIdentifier(){
        return this.raidId;
    }

    public @NotNull Collection<RaidMob> getAliveMobs(){
        return raidMobs.stream()
                .filter(RaidMob::isAlive)
                .toList();
    }

    public void addRaidMob(@NotNull RaidMob raidMob){
        raidMobs.add(raidMob);
    }

    public void removeRaidMob(@NotNull RaidMob raidMob){
        raidMobs.remove(raidMob);
    }

    public void setCompleted(boolean completed){
        this.isCompleted = completed;
    }


    private @Range(from = 0, to = 1) float getClampedProgress() {
        int mobAmount = raidMobs.size();

        if (mobAmount == 0) {
            return 0.0f;
        }

        long mobsAlive = raidMobs.stream()
                .filter(RaidMob::isAlive)
                .count();

        float progress = (float) mobsAlive / mobAmount;

        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private void tickEmotionControllers(){
        raidMobs.forEach(mob -> mob.getEmotionController().tick());
    }

    private void showBossBarToPlayers() {
        this.getEntitiesInAreaOfType(Player.class).forEach(
                player -> player.showBossBar(bossBar)
        );
    }

    private void congratulatePlayers() {
        this.getEntitiesInAreaOfType(Player.class).forEach(
                player -> player.sendMessage(
                        Component.text("Has ")
                                .color(NamedTextColor.GRAY)
                                .append(Component.text(" derrotado")
                                        .color(NamedTextColor.RED)
                                        .append(Component.text(" la")
                                                .color(NamedTextColor.GRAY)
                                                .append(Component.text(" raid")
                                                        .color(NamedTextColor.GOLD)
                                                        .append(Component.text("!")
                                                                .color(NamedTextColor.GRAY)
                                                        )
                                                )
                                        )
                                )
                )
        );
    }

    private void removeBossBarToPlayers() {
        this.getEntitiesInAreaOfType(Player.class).forEach(
                player -> player.hideBossBar(bossBar)
        );
    }

    private void checkForCompletion() {
        if(this.isCompleted){
            congratulatePlayers();
            this.stop();
        }
    }

    private void loadRaidMobs(
            @NotNull TCCPlugin plugin,
            @NotNull List<String> raidMobsStrings,
            @NotNull Collection<RaidMobDto> raidMobsDto,
            @NotNull Map<String, Integer> mobsWithAmount
    ) {
        for (String raidMobsString : raidMobsStrings) {
            var maybeRaidMobDto = raidMobsDto.stream().filter(dto -> dto.getRaidMobId().equalsIgnoreCase(raidMobsString)).findFirst();

            if(maybeRaidMobDto.isPresent()){
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

        if(raidMobsAmounts.length != raidMobsStrings.size()){
            throw new InputMismatchException("there should be an amount for each mob, lists size should match on raid dto");
        }

        Map<String, Integer> mobsWithAmount = new HashMap<>();

        for (int i = 0; i < raidMobsStrings.size(); i++) {
            String raidMobsString = raidMobsStrings.get(i);
            int amount = raidMobsAmounts[i];

            mobsWithAmount.put(raidMobsString, amount);
        }

        return mobsWithAmount;
    }

    @NotNull
    private static BossBar buildBossBar() {
        return BossBar.bossBar(
                Component.text("Progreso de la raid").color(NamedTextColor.RED),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.NOTCHED_12
        );
    }

}
