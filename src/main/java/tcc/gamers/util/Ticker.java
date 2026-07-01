package tcc.gamers.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Ticker<TaskType> {

    void tick();

    void stop();

    void start();

    @NotNull Optional<TaskType> getTask();
}
