package tcc.gamers.nms.nautilus;

import org.bukkit.entity.Nautilus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.tutorials.model.Model;

public class NautilusAnimationManager {

    public enum Animation {
        FLY_WALK("fly_walk"),
        FLY_WALK_LEFT("fly_walk_left"),
        FLY_WALK_RIGHT("fly_walk_right"),
        IDLE_GROUND("idle"),
        WALK_GROUND("walk"),
        FLY_IDLE("fly_idle"),
        BELLY("belly");

        private final String id;

        Animation(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private Model<Nautilus> model;
    private Animation currentAnimation;

    public void attachModel(@Nullable Model<Nautilus> model) {
        this.model = model;
    }

    public void play(@NotNull Animation animation) {
        if (model == null) return;

        if (this.currentAnimation != animation) {
            model.animate(animation.getId());
            this.currentAnimation = animation;
        }
    }

    public @NotNull Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public void forceResetState() {
        this.currentAnimation = null;
    }
}