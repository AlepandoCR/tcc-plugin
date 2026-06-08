package tcc.gamers.raid;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;

import java.util.Collection;

public class RaidLobbyTask extends BukkitRunnable {

    private final Raid raid;

    private int secondsWaiting = 0;
    private int forceStartTimer = 60;
    private int readyStartTimer = 5;

    public RaidLobbyTask(@NotNull Raid raid, @NotNull TCCPlugin plugin) {
        this.raid = raid;
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        if (raid.isCompleted()) {
            this.cancel();
            return;
        }

        Collection<Player> players = raid.getEntitiesInAreaOfType(Player.class);

        if (players.isEmpty()) {
            return;
        }

        int currentPlayers = players.size();
        int requiredPlayers = raid.getRequiredPlayersAmount();
        Component actionBarMessage;

        if (currentPlayers >= requiredPlayers) {
            if (readyStartTimer > 0) {
                actionBarMessage = Component.text("¡Iniciando en " + readyStartTimer + "s!")
                        .color(NamedTextColor.GREEN);

                float pitch = 1.0f + ((5 - readyStartTimer) * 0.2f);
                playSoundToAll(players, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);

                readyStartTimer--;
            } else {
                startRaid(players);
                return;
            }
        }
        else {
            readyStartTimer = 5;
            secondsWaiting++;

            if (secondsWaiting <= 15) {
                actionBarMessage = Component.text("Jugadores Necesarios : " + currentPlayers + "/" + requiredPlayers)
                        .color(NamedTextColor.YELLOW);
            } else {
                if (forceStartTimer > 0) {

                    if (forceStartTimer % 15 == 0) {
                        actionBarMessage = Component.text("¡Iniciará en " + forceStartTimer + "s aunque falten jugadores!")
                                .color(NamedTextColor.RED);
                        playSoundToAll(players, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.7f);
                    }
                    else if (forceStartTimer <= 5) {
                        actionBarMessage = Component.text("¡Iniciará en " + forceStartTimer + "s aunque falten jugadores!")
                                .color(NamedTextColor.RED);

                        float pitch = 1.0f + ((5 - forceStartTimer) * 0.2f);
                        playSoundToAll(players, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                    }
                    else {
                        actionBarMessage = Component.text("Jugadores Necesarios : " + currentPlayers + "/" + requiredPlayers)
                                .color(NamedTextColor.GRAY).append(Component.text(" (" + forceStartTimer + "s 💀)")
                                        .color(NamedTextColor.GOLD));
                    }

                    forceStartTimer--;
                } else {
                    startRaid(players);
                    return;
                }
            }
        }

        players.forEach(player -> player.sendActionBar(actionBarMessage));
    }

    private void playSoundToAll(@NotNull Collection<Player> players, Sound sound, float volume, float pitch) {
        players.forEach(player -> player.playSound(player.getLocation(), sound, volume, pitch));
    }

    private void startRaid(@NotNull Collection<Player> players) {
        players.forEach(player -> player.sendActionBar(Component.empty()));

        playSoundToAll(players, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

        raid.spawnMobs();
        raid.startRaidTickLogic();
        this.cancel();
    }
}