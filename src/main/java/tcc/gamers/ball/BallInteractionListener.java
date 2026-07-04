package tcc.gamers.ball;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.item.ball.SoccerBallHelper;
import tcc.gamers.item.ball.SoccerCleatsHelper;

public class BallInteractionListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    public BallInteractionListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBallKick(@NotNull PlayerToggleSneakEvent event){

        var player = event.getPlayer();

        if(event.isSneaking()){
            if(SoccerCleatsHelper.playerHasCleats(player)){
                plugin.getBallManager().getClosesBallTo(player.getLocation(),2.7).ifPresent(ball ->{
                            ball.getController().getKickController().tryKick(player);

                            plugin.getBallManager().getSafeBalls().forEach(otherBall -> {
                                if (!otherBall.getUUID().equals(ball.getUUID())){
                                    otherBall.getController().getKickController().removeCharge(player.getUniqueId()); // remove charging for other balls, only current one
                                }
                            });
                        }
                );
            }
        }
    }


    @EventHandler
    public void onPlayerSpawnBall(@NotNull PlayerInteractEvent event){
        var player = event.getPlayer();

        var world = player.getWorld();

        if(event.getAction().isRightClick()){
            var itemInHand = player.getInventory().getItemInMainHand();
            if(SoccerBallHelper.isSoccerBall(itemInHand)){
                itemInHand.subtract();
                var ball = new Ball(world ,plugin);

                ball.spawn(player.getLocation());

                plugin.getBallManager().registerBall(ball);
            }
        }
    }

    @EventHandler
    public void onPlayerUnequipCleats(@NotNull PlayerArmorChangeEvent event){
        if(event.getSlot().equals(EquipmentSlot.FEET)){
            var player = event.getPlayer();
            if(!SoccerCleatsHelper.playerHasCleats(player)){
                plugin
                        .getBallManager()
                        .getSafeBalls()
                        .forEach(
                                ball -> ball
                                        .getController()
                                        .getKickController()
                                        .removeCharge(player.getUniqueId()
                                        )
                        );
            }
        }
    }
}
