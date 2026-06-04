package tcc.gamers.util;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Ticker {

    void tick();

    void stop();

    void start();

    @NotNull Optional<BukkitTask> getTask();
}
