package tcc.gamers.ball;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.Ticker;

import java.util.Optional;


public class BallKickCharge implements Ticker<ScheduledTask> {

    public static double MAX_KICK_CHARGE = 3000;

    private static final int MAX_BARS = 20;

    private double charge = 0;

    private final @NotNull LivingEntity managedEntity;

    private final @NotNull TCCPlugin plugin;

    private @Nullable ScheduledTask task = null;

    public BallKickCharge(@NotNull LivingEntity managedEntity, @NotNull TCCPlugin plugin) {
        this.managedEntity = managedEntity;
        this.plugin = plugin;
    }

    @Override
    public void tick() {
        if (!this.managedEntity.isValid()) {
            this.stop();
            return;
        }

        if (this.charge < MAX_KICK_CHARGE) {
            this.charge += 20;
        }

        if (this.managedEntity instanceof Player player) {
            sendChargeActionBar(player);
        }
    }

    @Override
    public void stop() {
        if(task != null){
            task.cancel();
        }
    }

    @Override
    public void start() {
        task = this.managedEntity.getScheduler().runAtFixedRate(plugin, // run timer in entity
                (_ -> this.tick()),
                this::stop,
                1L,
                1L
        );
    }

    @NotNull
    @Override
    public Optional<ScheduledTask> getTask() {
        return Optional.ofNullable(task);
    }

    public @NotNull LivingEntity getManagedEntity() {
        return managedEntity;
    }

    public double getCharge() {
        return charge;
    }

    public double consumeCharge(){
        var current = charge;

        charge = 0;

        return current;
    }

    private void sendChargeActionBar(@NotNull Player player) {
        double percentage = Math.min(1.0, charge / MAX_KICK_CHARGE);

        Component actionBar = Component.text("⚽ ").color(NamedTextColor.GOLD)
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(generateChargeBar(percentage))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f%%", percentage * 100))
                        .color(getColorForThreshold(percentage)));

        player.sendActionBar(actionBar);
    }

    @NotNull
    private Component generateChargeBar(double percentage) {
        int activeBars = (int) Math.round(percentage * MAX_BARS);
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < MAX_BARS; i++) {
            boolean active = i < activeBars;
            TextColor color = active ? getColorForThreshold((double) i / MAX_BARS) : NamedTextColor.DARK_GRAY;
            builder.append(Component.text(active ? "■" : "□").color(color));
        }

        return builder.build();
    }

    @NotNull
    private TextColor getColorForThreshold(double ratio) {
        if (ratio < 0.33) return NamedTextColor.GREEN;
        if (ratio < 0.66) return NamedTextColor.YELLOW;
        if (ratio < 0.85) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }
}