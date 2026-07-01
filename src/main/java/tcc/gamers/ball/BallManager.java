package tcc.gamers.ball;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import tcc.gamers.TCCPlugin;

import java.util.*;

public class BallManager {

    private final @NotNull TCCPlugin plugin;

    // entities already have their uuid,
    // but we want to check if the entity is still instance of ball since minecraft
    // reloads the entity as its base implementation rather than custom
    private final @NotNull Map<UUID, Ball> ballMap;

    public BallManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
        this.ballMap = new HashMap<>();
    }

    public void registerBall(@NotNull Ball ball) {
        ballMap.put(ball.getUUID(), ball);
        trimBalls();
    }

    public void removeBall(@NotNull Ball ball){
        removeBall(ball.getUUID());
    }

    public void removeBall(@NotNull UUID uuid){
        ballMap.remove(uuid);
        trimBalls();
    }

    public @NotNull @Unmodifiable Collection<Ball> getSafeBalls(){
        var uuids = ballMap.keySet();

        var safeBalls = new ArrayList<Ball>();

        uuids.forEach(uuid -> {
            var entity = plugin.getServer().getEntity(uuid);

            if(entity != null){
               getSafeBall(entity).ifPresent(safeBalls::add);
            }
        });

        return Collections.unmodifiableCollection(safeBalls);
    }

    public @NotNull Optional<Ball> getSafeBall(@NotNull UUID uuid){
        var entity = plugin.getServer().getEntity(uuid);

        if(entity == null){
            return Optional.empty();
        }

        return getSafeBall(entity);
    }

    public @NotNull Optional<Ball> getSafeBall(@NotNull Entity entity){

        var nmsEntity = ((CraftEntity) entity).getHandle();

        if(nmsEntity instanceof Ball ball){
            // we know the ball already has our properties in it
            return Optional.of(ball);
        }

        return Optional.empty();
    }

    public @NotNull Optional<Ball> getClosesBallTo(@NotNull Location location, double radius){
        return location.getNearbyEntities(radius,radius,radius).stream().filter(entity -> getSafeBall(entity).isPresent())
                .map(entity -> getSafeBall(entity).orElseThrow())// shouldn't throw since we filtered for present
                .min(Comparator.comparingDouble(ball -> ball.getBukkitEntity().getLocation().distanceSquared(location)));
    }

    // 7u7
    // cleans stored uuids that no longer hold ball properties
    public void trimBalls(){
        ballMap.keySet().removeIf(uuid -> getSafeBall(uuid).isEmpty());
    }
}
