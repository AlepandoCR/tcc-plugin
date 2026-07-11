package tcc.gamers.ball.physics;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ball.BallController;
import tcc.gamers.ball.BallKickCharge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KickController {

    private final @NotNull List<BallKickCharge> kickCharges;

    private final @NotNull TCCPlugin plugin;

    public KickController(@NotNull TCCPlugin plugin) {
        this.kickCharges = new ArrayList<>();
        this.plugin = plugin;
    }

    public void tryKick(@NotNull LivingEntity kicker, @NotNull BallController controller){
        BallKickCharge charge = kickCharges.stream()
                .filter(c -> c.getManagedEntity().getUniqueId().equals(kicker.getUniqueId()))
                .findFirst()
                .orElse(null);

        if(charge == null){
            charge = new BallKickCharge(kicker, plugin, () -> removeCharge(kicker.getUniqueId()));
            charge.start();
            kickCharges.add(charge);
        }
        else{
            var distance = kicker.getLocation().distance(controller.getBukkitBall().getLocation());
            if(distance <= 2.7){
                controller.getPhysicManager().kickBall(kicker, charge.consumeCharge());
            }
        }
        trimCharges(controller);
    }

    public void trimCharges(@NotNull BallController controller){
        var near = controller.getBukkitBall().getNearbyEntities(45, 45, 45);
        kickCharges.removeIf(charge -> {
            boolean stale = !near.contains(charge.getManagedEntity());
            if (stale) charge.stop();
            return stale;
        });
    }

    public void removeCharge(@NotNull UUID managedEntityUniqueIdentifier){
        kickCharges.stream().filter(charge -> charge.getManagedEntity().getUniqueId().equals(managedEntityUniqueIdentifier))
                .findFirst()
                .ifPresent(
                        ballKickCharge -> {
                            ballKickCharge.stop();
                            kickCharges.remove(ballKickCharge);
                        }
                );
    }

}