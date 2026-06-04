package tcc.gamers.area;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.util.Ticker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public abstract class SupervisedArea extends Area implements Ticker {

    protected final @NotNull TCCPlugin plugin;

    private @Nullable BukkitTask task;

    private final @NotNull BukkitRunnable runnable;

    protected final @NotNull Collection<Entity> entitiesInArea;

    private final long tickFrequency;

    protected boolean onTick = false;

    public SupervisedArea(
            @NotNull Location center,
            @NotNull String identifier,
            @NotNull TCCPlugin plugin,
            double xRadius,
            double yRadius,
            double zRadius,
            long tickFrequency
    ) {
        super(center, identifier, xRadius, yRadius, zRadius);
        this.plugin = plugin;
        this.tickFrequency = tickFrequency;
        this.runnable = buildRunnable(); // each 5 ticks
        this.task = null; // first start call will initiate the task
        this.entitiesInArea = new ArrayList<>();
    }

    public SupervisedArea(
            @NotNull Area area,
            @NotNull TCCPlugin plugin,
            long tickFrequency
    ){
        this(
                area.getCenter(),
                area.getIdentifier(),
                plugin, area.getXRadius(),
                area.getYRadius(),
                area.getZRadius(),
                tickFrequency
        );
    }

    protected abstract void onTick();

    protected abstract void onStop();

    protected abstract void onStart();

    @Override
    public void tick() {
        entitiesInArea.removeIf(entity -> !contains(entity)); // cleanup removal

        getCenter().getNearbyEntities(
                getXRadius(),
                getYRadius(),
                getZRadius()
        ).forEach(entity -> { // check with same radius
            if(contains(entity)) {
                entitiesInArea.add(entity);
            } else {
                entitiesInArea.remove(entity);
            }
        });



        if(onTick){
            onTick();
        }else{
            // show borders to players inside
            getEntitiesInAreaOfType(Player.class)
                    .forEach(this::visualize);
        }
    }



    @Override
    public void stop() {
        if(task != null && !task.isCancelled()) {
            task.cancel();
        }

        onStop();
    }

    @Override
    public void start() {
       if(this.task == null) {
            task = runnable.runTaskTimer(plugin, 0, tickFrequency);
       }

       onStart();
    }

    @Override
    public @NotNull Optional<BukkitTask> getTask() {
        return Optional.ofNullable(task); // task may be there... or not
    }

    public @NotNull Collection<Entity> getEntitiesInArea() {
        return entitiesInArea;
    }

    public <EntityType extends Entity> @NotNull Collection<EntityType> getEntitiesInAreaOfType(@NotNull Class<EntityType> type) {
        Collection<EntityType> filtered = new ArrayList<>();
        entitiesInArea
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .forEach(filtered::add);

        return filtered;
    }

    private @NotNull BukkitRunnable buildRunnable() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
    }
}

