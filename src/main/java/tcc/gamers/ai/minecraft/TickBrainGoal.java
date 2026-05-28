package tcc.gamers.ai.minecraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongConsumer;

public class TickBrainGoal<LivingEntityType extends LivingEntity> extends Goal {

    private final @NotNull LivingEntityType entity;
    private final @NotNull Brain<LivingEntityType> castedBrain;
    private final @NotNull LongConsumer customTickLogic;

    private long tickCounter = 0;

    @SuppressWarnings("unchecked")
    public TickBrainGoal(
            @NotNull LivingEntityType entity,
            @NotNull LongConsumer customTickLogic
    ) {
        this.entity = entity;
        this.castedBrain = (Brain<LivingEntityType>) entity.getBrain();
        this.customTickLogic = customTickLogic;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        tickCounter++;

        try {
            customTickLogic.accept(tickCounter);
        } catch (Throwable ignored) {
        }

        if (entity.isRemoved() || !entity.isAlive()) {
            return;
        }

        castedBrain.tick(
                (ServerLevel) entity.level(),
                entity
        );
    }
}