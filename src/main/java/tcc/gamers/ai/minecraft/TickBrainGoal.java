package tcc.gamers.ai.minecraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.nms.horses.CustomSpartanHorse;

public class TickBrainGoal extends Goal {

    private static final double DESPAWN_DISTANCE = 45.0;
    private static final int DESPAWN_CHECK_PERIOD_TICKS = 20;

    private final CustomSpartanHorse<?> horse;

    private final Brain<CustomSpartanHorse<?>> castedBrain;

    private long tickCounter = 0;

    @SuppressWarnings("unchecked")
    public TickBrainGoal(@NotNull CustomSpartanHorse<?> horse) {
        this.horse = horse;
        this.castedBrain =  ((Brain<CustomSpartanHorse<?>>)horse.getBrain());
    }

    @Override
    public boolean canUse() {
        return true;
    }
    @Override

    public void tick() {
        tickCounter++;

        if (tickCounter % DESPAWN_CHECK_PERIOD_TICKS == 0 && horse.isFarFromAllPlayers(DESPAWN_DISTANCE)) {
            horse.despawnHorse();
            return;
        }

        // Advance horse controllers (speed smoothing) before the brain ticks so behaviors observe updated values
        try {
            horse.controllerTick();
        } catch (Throwable ignored) {
        }

        castedBrain.tick(
                (ServerLevel)horse.level(),
                horse
        );
    }
}
