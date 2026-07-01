package tcc.gamers.ball.physics;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ball.BallController;
import tcc.gamers.ball.BallKickCharge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KickController {

    private final BallController controller;

    private final @NotNull List<BallKickCharge> kickCharges;

    private final @NotNull TCCPlugin plugin;

    public KickController(@NotNull TCCPlugin plugin, @NotNull BallController controller) {
        this.controller = controller;
        this.kickCharges = new ArrayList<>();
        this.plugin = plugin;
    }

    public void tryKick(@NotNull LivingEntity kicker){
        BallKickCharge charge = kickCharges.stream()
                .filter(c -> c.getManagedEntity().getUniqueId().equals(kicker.getUniqueId()))
                .findFirst()
                .orElse(null);

        if(charge == null){
            charge = new BallKickCharge(kicker, plugin);
            charge.start();
            kickCharges.add(charge);
        }
        else{
            controller.getPhysicManager().kickBall(kicker, charge.consumeCharge());
        }
        trimCharges();
    }

    public void trimCharges(){
        // clean charge manager for entities too far away
        var near = controller.getBukkitBall().getNearbyEntities(45, 45, 45);

        kickCharges.forEach(charge -> {
            if(!near.contains(charge.getManagedEntity())){
                removeCharge(charge.getManagedEntity().getUniqueId());
            }
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