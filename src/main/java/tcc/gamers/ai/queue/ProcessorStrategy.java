package tcc.gamers.ai.queue;

import org.jetbrains.annotations.NotNull;
import org.spartan.api.engine.SpartanModel;
import org.spartan.api.engine.config.RecurrentSoftActorCriticConfig;

public interface ProcessorStrategy {

    /**
     * Advance the model by one step for the current state already written into
     * {@code evaluationState}.
     *
     */
    void execute(
            @NotNull SpartanModel<RecurrentSoftActorCriticConfig> model,
            @NotNull CurrentEvaluationState evaluationState
    );

    @NotNull String label();


    record Training() implements ProcessorStrategy {

        /** Shared singleton — no state, safe to reuse across any number of managers. */
        public static final Training INSTANCE = new Training();

        @Override
        public void execute(
                @NotNull SpartanModel<RecurrentSoftActorCriticConfig> model,
                @NotNull CurrentEvaluationState evaluationState
        ) {
            model.tick();
        }

        @Override
        public @NotNull String label() {
            return DragonBrainMode.TRAINING.name();
        }
    }

    /**
     * Inference-only strategy: delegates to {@code model.tick()} but signals to
     * the model that no learning should occur this step.
     */
    record Inference() implements ProcessorStrategy {

        /** Shared singleton — no state, safe to reuse across any number of managers. */
        public static final Inference INSTANCE = new Inference();

        @Override
        public void execute(
                @NotNull SpartanModel<RecurrentSoftActorCriticConfig> model,
                @NotNull CurrentEvaluationState evaluationState
        ) {
            model.tick();
        }

        @Override
        public @NotNull String label() {
            return DragonBrainMode.INFERENCE.name();
        }
    }

    /**
     * Top-level mode selector passed to {@link DragonQueueManager} at construction.
     * Resolved to a {@link ProcessorStrategy} by the manager's factory.
     */
    enum DragonBrainMode {

        /**
         * The model learns from every tick — weights are updated online.
         * Maps to {@link Training}.
         */
        TRAINING,

        /**
         * The model uses frozen weights for pure exploitation.
         * Maps to {@link Inference}.
         */
        INFERENCE;

        /** @return The canonical strategy implementation for this mode. */
        public @NotNull ProcessorStrategy toStrategy() {
            return switch (this) {
                case TRAINING  -> Training.INSTANCE;
                case INFERENCE -> Inference.INSTANCE;
            };
        }
    }
}