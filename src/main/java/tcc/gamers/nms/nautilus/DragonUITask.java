package tcc.gamers.nms.nautilus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ui.AbstractActionBarTask;

import java.util.Optional;


public class DragonUITask {

    private static final int    MAX_BARS    = 15;
    private static final String[] SPEEDOMETER = {"🕛", "🕧", "🕜", "🕑", "🕝", "🕒"};

    private final @NotNull TCCPlugin               plugin;
    private final @NotNull AbstractDragonNautilus  dragon;
    private final @NotNull Player                  rider;

    private @Nullable BukkitTask activeTask = null;
    private double previousEnergyRaw = 0.0;

    public DragonUITask(
            @NotNull TCCPlugin plugin,
            @NotNull AbstractDragonNautilus dragon,
            @NotNull Player rider
    ) {
        this.plugin = plugin;
        this.dragon = dragon;
        this.rider  = rider;
    }

    /**
     * Starts the HUD ticker. Safe to call even if already running — the existing
     * task is canceled first and a fresh one is created.
     */
    public @NotNull BukkitTask start() {
        stop(); // cancel any existing task before creating a new one
        previousEnergyRaw = 0.0;

        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick(this);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return activeTask;
    }

    public void stop() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    public @NotNull Optional<BukkitTask> getTask() {
        return Optional.ofNullable(activeTask);
    }

    public boolean isRunning() {
        return activeTask != null;
    }

    private void tick(@NotNull BukkitRunnable self) {
        if (!dragon.isAlive() || !rider.isInsideVehicle() || dragon.getBukkitNautilus() != rider.getVehicle()) {
            sendActionBar(Component.empty());
            self.cancel();
            activeTask = null;
            return;
        }

        double currentSpeed = dragon.getDeltaMovement().length();
        double bps = currentSpeed * 20.0;
        double forwardSpeed = dragon.getCurrentForwardSpeed();
        double diveEnergy = dragon.getStoredDiveEnergy();
        double maxExpectedSpeed = 5.0;
        double totalEnergyRaw = forwardSpeed + (diveEnergy * 0.8);
        double energyPercentage = Math.min(1.0, totalEnergyRaw / maxExpectedSpeed);
        double energyDelta = totalEnergyRaw - previousEnergyRaw;
        this.previousEnergyRaw = totalEnergyRaw;

        String speedometerEmoji = getSpeedometerEmoji(energyPercentage);
        Component visualBar = generateEnergyBar(energyPercentage);
        Component trendIndicator = generateTrendIndicator(energyDelta);

        Component actionBar = Component.text(speedometerEmoji + " ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.1f BPS  ", bps)).color(NamedTextColor.WHITE))
                .append(trendIndicator)
                .append(Component.text("  "))
                .append(visualBar);

        sendActionBar(actionBar);
    }


    private void sendActionBar(@NotNull Component component) {
        rider.sendActionBar(component);
    }

    @NotNull
    private Component generateTrendIndicator(double delta) {
        if (delta > 0.02) {
            return Component.text(String.format("▲ +%.2f", delta)).color(NamedTextColor.GREEN);
        } else if (delta < -0.02) {
            return Component.text(String.format("▼ %.2f", delta)).color(NamedTextColor.RED);
        } else {
            return Component.text("▬ 0.00").color(NamedTextColor.GRAY);
        }
    }

    private String getSpeedometerEmoji(double percentage) {
        int index = (int) Math.round(percentage * (SPEEDOMETER.length - 1));
        index = Math.max(0, Math.min(index, SPEEDOMETER.length - 1));
        return SPEEDOMETER[index];
    }

    @NotNull
    private Component generateEnergyBar(double percentage) {
        int activeBars = (int) Math.round(percentage * MAX_BARS);
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < MAX_BARS; i++) {
            TextColor color = (i < activeBars) ? getColorForThreshold(i) : NamedTextColor.DARK_GRAY;
            builder.append(Component.text("█").color(color));
        }
        return builder.build();
    }

    private TextColor getColorForThreshold(int index) {
        double ratio = (double) index / MAX_BARS;
        if (ratio < 0.33) return NamedTextColor.GREEN;
        if (ratio < 0.66) return NamedTextColor.YELLOW;
        if (ratio < 0.85) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }
}