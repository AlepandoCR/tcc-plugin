package tcc.gamers.ball.dash;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.Ticker;

import java.util.Optional;

/**
 * Tracks a single player's dash cooldown and displays its progress
 * through a boss bar. Self-expires via {@code onExpire} once the
 * cooldown elapses or the player goes offline.
 */
public class DashCooldown implements Ticker<ScheduledTask> {

    public static long COOLDOWN_DURATION_MILLIS = 10_000L;

    private static final String BOSS_BAR_TITLE = "Dash Cooldown";

    private final @NotNull Player player;

    private final @NotNull TCCPlugin plugin;

    private final @NotNull BossBar bossBar;

    private final long startTime;

    private @Nullable ScheduledTask task = null;

    private final @Nullable Runnable onExpire;

    public DashCooldown(@NotNull Player player, @NotNull TCCPlugin plugin, @Nullable Runnable onExpire) {
        this.player = player;
        this.plugin = plugin;
        this.onExpire = onExpire;
        this.startTime = System.currentTimeMillis();
        this.bossBar = plugin.getServer().createBossBar(BOSS_BAR_TITLE, BarColor.BLUE, BarStyle.SEGMENTED_10);
    }

    @Override
    public void tick() {
        if (!player.isOnline()) {
            expire();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= COOLDOWN_DURATION_MILLIS) {
            expire();
            return;
        }

        double progress = (double) elapsed / COOLDOWN_DURATION_MILLIS;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    private void expire() {
        stop();
        if (onExpire != null) onExpire.run();
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
        }
        bossBar.removeAll();
    }

    @Override
    public void start() {
        bossBar.addPlayer(player);
        task = player.getScheduler().runAtFixedRate(plugin,
                (_ -> this.tick()),
                this::stop,
                5L,
                5L
        );
    }

    @NotNull
    @Override
    public Optional<ScheduledTask> getTask() {
        return Optional.ofNullable(task);
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public long getRemainingMillis() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0L, COOLDOWN_DURATION_MILLIS - elapsed);
    }

    public int getRemainingSeconds() {
        return (int) Math.ceil(getRemainingMillis() / 1000.0);
    }
}