package tcc.gamers.race;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.minecraft.world.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.race.dragon.DragonRace;

public class RaceListener implements Listener {

    private final @NotNull TCCPlugin plugin;

    public RaceListener(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreparingRace(@NotNull EntityMoveEvent event){
        var entity = event.getEntity();
        plugin.getRaceManager().getRacesOfType(DragonRace.class).stream()
                .filter(DragonRace::isPreparing)
                .forEach(race -> {
                    if (race.getDragons().stream().map(Entity::getUUID).toList().contains(entity.getUniqueId())){
                        event.setCancelled(true);
                    }
                });
    }

    @EventHandler
    public void onPlayerDismount(@NotNull EntityDismountEvent event){
        var passenger = event.getEntity();
        var vehicle = event.getDismounted();

        plugin.getRaceManager().getRacesOfType(DragonRace.class).stream()
                .filter(DragonRace::isPreparing)
                .forEach(race -> {
                    if (race.getDragons().stream().map(Entity::getUUID).toList().contains(vehicle.getUniqueId())){
                        if (race.isParticipant(passenger.getUniqueId())){
                            event.setCancelled(true);
                        }
                    }
                });
    }

    @EventHandler
    public void onDragonDamage(@NotNull EntityDamageEvent event) {
        var entity = event.getEntity();

        plugin.getRaceManager().getRacesOfType(DragonRace.class)
                .forEach(race -> {
                    if (race.getDragons().stream().map(Entity::getUUID).toList().contains(entity.getUniqueId())) {
                        event.setCancelled(true);
                    }
                });
    }
}