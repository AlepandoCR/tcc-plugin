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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BallInteractionListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    private final @NotNull Map<UUID, Long> spawnCooldowns;

    public BallInteractionListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.spawnCooldowns = new HashMap<>();
    }

    @EventHandler
    public void onBallKick(@NotNull PlayerToggleSneakEvent event){

        var player = event.getPlayer();

        if(event.isSneaking()){
            if(SoccerCleatsHelper.playerHasCleats(player)){
                plugin.getBallManager()
                        .getClosesBallTo(
                                player.getLocation(),
                                40
                        ).ifPresent(ball ->
                                ball.getController()
                                        .getKickController()
                                        .tryKick(
                                                player,
                                                ball.getController()
                                        )
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
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();

                if(spawnCooldowns.containsKey(playerId)){
                    long lastSpawnTime = spawnCooldowns.get(playerId);
                    long timeElapsed = currentTime - lastSpawnTime;

                    long cooldownTime = 250L;
                    if (timeElapsed < cooldownTime) {
                        event.setCancelled(true);
                        return;
                    }
                }

                itemInHand.subtract();
                var ball = new Ball(world, plugin);

                ball.spawn(player.getLocation());

                plugin.getBallManager().registerBall(ball);

                spawnCooldowns.put(playerId, currentTime);
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