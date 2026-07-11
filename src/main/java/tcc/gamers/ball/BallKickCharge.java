package tcc.gamers.ball;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.item.ball.SoccerCleatsHelper;
import tcc.gamers.util.Ticker;

import java.util.Optional;


public class BallKickCharge implements Ticker<ScheduledTask> {

    public static double MAX_KICK_CHARGE = 3000;

    private static final int MAX_BARS = 20;

    private double charge = 0;

    private final @NotNull LivingEntity managedEntity;

    private final @NotNull TCCPlugin plugin;

    private @Nullable ScheduledTask task = null;

    private final @Nullable Runnable onExpire;

    public BallKickCharge(@NotNull LivingEntity managedEntity, @NotNull TCCPlugin plugin, @Nullable Runnable onExpire) {
        this.managedEntity = managedEntity;
        this.plugin = plugin;
        this.onExpire = onExpire;
    }

    public void tick() {
        if (!this.managedEntity.isValid()) { expire(); return; }
        if (this.managedEntity instanceof Player player && !SoccerCleatsHelper.playerHasCleats(player)) { expire(); return; }

        double maxCharge = getMaxChargeForSlot();

        if (this.charge > maxCharge) {
            this.charge = maxCharge;
        } else if (this.charge < maxCharge) {
            this.charge += 20;
        }

        if (this.managedEntity instanceof Player player) {
            sendChargeActionBar(player, maxCharge);
        }
    }

    private double getMaxChargeForSlot() {
        if (this.managedEntity instanceof Player player) {
            int slot = player.getInventory().getHeldItemSlot(); // 0-8
            double segment = MAX_KICK_CHARGE / 9.0;
            return segment * (slot + 1);
        }
        return MAX_KICK_CHARGE;
    }

    private void expire() {
        stop();
        if (onExpire != null) onExpire.run();
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

    private void sendChargeActionBar(@NotNull Player player,  double maxCharge) {
        double percentage = Math.min(1.0, charge / MAX_KICK_CHARGE);
        double capPercentage = Math.min(1.0, maxCharge / MAX_KICK_CHARGE);

        Component actionBar = Component.text("⚽ ").color(NamedTextColor.GOLD)
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(generateChargeBar(percentage, capPercentage))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f%%", percentage * 100))
                        .color(getColorForThreshold(percentage)));

        player.sendActionBar(actionBar);
    }

    @NotNull
    private Component generateChargeBar(double percentage, double capPercentage) {
        int activeBars = (int) Math.round(percentage * MAX_BARS);
        int capBar = (int) Math.min(MAX_BARS - 1, Math.round(capPercentage * MAX_BARS));
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < MAX_BARS; i++) {
            boolean active = i < activeBars;
            boolean isCapMarker = !active && i == capBar && capPercentage < 1.0;

            var component = getComponentForProgress(active, i, isCapMarker);

            builder.append(component);
        }

        return builder.build();
    }

    @NotNull
    private TextComponent getComponentForProgress(boolean active, double i, boolean isCapMarker) {
        TextColor color = active
                ? getColorForThreshold(i / MAX_BARS)
                : (isCapMarker ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY);

        TextDecoration textDecoration = isCapMarker ? TextDecoration.BOLD : null;

        var component = Component.text(active ? "■" : (isCapMarker ? "│" : "□")).color(color);

        if(textDecoration != null) {
            component = component.decorate(textDecoration);
        }
        return component;
    }

    @NotNull
    private TextColor getColorForThreshold(double ratio) {
        if (ratio < 0.33) return NamedTextColor.GREEN;
        if (ratio < 0.66) return NamedTextColor.YELLOW;
        if (ratio < 0.85) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }
}