package tcc.gamers.ai.spartan.action;

import org.jetbrains.annotations.NotNull;
import tcc.gamers.ai.spartan.action.reward.SpartanRewarder;

public class DragonLookAction extends AbstractLookAction{



    private final DragonLookAction.Direction direction;
    public DragonLookAction(@NotNull SpartanRewarder rewarder, @NotNull DragonLookAction.Direction direction) {
        super(rewarder);
        this.direction = direction;
    }


    @NotNull
    @Override
    public String identifier() {
        return "dragon_look_action_" + direction.getName();
    }

    public enum Direction {

       YAW("yaw"),
        PITCH("pitch");

        private final String name;
        Direction (@NotNull String name) {
            this.name = name;
        }

        public @NotNull String getName() {
            return name;
        }
    }
}
