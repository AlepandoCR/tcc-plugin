package tcc.gamers.race;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;

import java.util.Collection;

public class RaceLobbyTask extends BukkitRunnable {

    private final Race<?> race;
    private int startTimer = 90;

    public RaceLobbyTask(@NotNull Race<?> race, @NotNull TCCPlugin plugin) {
        this.race = race;
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        Collection<Player> players = race.getEntitiesInAreaOfType(Player.class);

        if (startTimer == 10) {
            race.prepareRace();
        }

        if (startTimer <= 0) {
            startRaceTask(players);
            return;
        }

        Component actionBarMessage;

        if (startTimer <= 5) {
            actionBarMessage = Component.text("¡Iniciando carrera en " + startTimer + "s!")
                    .color(NamedTextColor.GREEN);

            float pitch = 1.0f + ((5 - startTimer) * 0.2f);
            playSoundToAll(players, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
        } else {
            NamedTextColor color = (startTimer % 15 == 0 || startTimer == 10) ? NamedTextColor.YELLOW : NamedTextColor.GRAY;

            actionBarMessage = Component.text("¡Iniciando carrera en " + startTimer + "s!")
                    .color(color);

            if (color == NamedTextColor.YELLOW) {
                playSoundToAll(players, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }

        players.forEach(player -> player.sendActionBar(actionBarMessage));

        if (startTimer <= 10) {
            Component mainTitle = Component.text(" ");
            Component subtitle = Component.text(startTimer + "s")
                    .color(startTimer <= 5 ? NamedTextColor.GREEN : NamedTextColor.YELLOW);

            Title title = Title.title(mainTitle, subtitle);
            players.forEach(player -> player.showTitle(title));
        }

        startTimer--;
    }

    private void playSoundToAll(@NotNull Collection<Player> players, Sound sound, float volume, float pitch) {
        players.forEach(player -> player.playSound(player.getLocation(), sound, volume, pitch));
    }

    private void startRaceTask(@NotNull Collection<Player> players) {
        players.forEach(Audience::clearTitle);
        playSoundToAll(players, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

        race.startRace();
        this.cancel();
    }
}