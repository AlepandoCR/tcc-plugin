package tcc.gamers.ai.spartan.action;

import org.jetbrains.annotations.NotNull;
import org.spartan.api.engine.action.type.SpartanAction;
import tcc.gamers.ai.spartan.action.reward.SpartanRewarder;

public abstract class AbstractLookAction implements SpartanAction {

    private final SpartanRewarder rewarder;

    protected double angle;

    public AbstractLookAction(@NotNull SpartanRewarder rewarder) {
        this.rewarder = rewarder;
        this.angle = 0.0;
    }

    @Override
    public double taskMaxMagnitude() {
        return 1.0;
    }

    @Override
    public double taskMinMagnitude() {
        return -1.0;
    }

    @Override
    public double award(){
        return rewarder.reward();
    }

    @Override
    public void task(double normalizedMagnitude) {
        angle = normalizedMagnitude * 180.0;
    }

    public double getAngle() {
        return angle;
    }
}
