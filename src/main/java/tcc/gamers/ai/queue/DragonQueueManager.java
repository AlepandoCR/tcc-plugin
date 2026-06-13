package tcc.gamers.ai.queue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spartan.api.engine.SpartanModel;
import org.spartan.api.engine.config.RecurrentSoftActorCriticConfig;
import org.spartan.api.engine.context.element.SpartanSingleContextElement;
import org.spartan.api.exception.SpartanNativeException;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.DragonModelMetrics;
import tcc.gamers.ai.queue.ProcessorStrategy.DragonBrainMode;
import tcc.gamers.ai.spartan.action.DragonLookAction;
import tcc.gamers.ai.spartan.action.reward.SpartanRewarder;
import tcc.gamers.ai.spartan.model.SpartanModelManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;
import java.util.logging.Logger;
/**
 * The central "Shared Brain" processor for all autonomous dragons.
 */
public final class DragonQueueManager {

    private static final long PROCESSING_WARN_THRESHOLD_MS = 50L;

    private final @NotNull TCCPlugin plugin;
    private final @NotNull Logger logger;
    private final @NotNull DragonBrainMode mode;
    private final @NotNull ProcessorStrategy strategy;


    private final @NotNull CurrentEvaluationState evaluationState = new CurrentEvaluationState();

    private @Nullable SpartanModel<RecurrentSoftActorCriticConfig> model;


    private @Nullable DragonLookAction sharedYawAction;
    private @Nullable DragonLookAction sharedPitchAction;

    private @Nullable Path modelSavePath;


    private final ConcurrentLinkedQueue<DragonStateRequest> requestQueue = new ConcurrentLinkedQueue<>();

    private final AtomicInteger totalProcessed = new AtomicInteger(0);

    private final AtomicInteger slowTickCount = new AtomicInteger(0);

    private final @NotNull DragonModelMetrics modelMetrics = new DragonModelMetrics();

    public DragonQueueManager(@NotNull TCCPlugin plugin, @NotNull DragonBrainMode mode) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mode = mode;
        this.strategy = mode.toStrategy();
    }


    /**
     * Builds the shared model, wires the context suppliers against
     * {@link #evaluationState}, and registers the shared action handles.
     *
     * @throws SpartanNativeException if the Spartan native engine fails to
     *  initialize the model.
     */
    public void initialize() throws SpartanNativeException {
        logger.info("[DragonQueueManager] Initializing shared brain in mode: " + mode);

        final SpartanRewarder sharedRewarder = () -> evaluationState.currentReward;

        this.sharedYawAction   = new DragonLookAction(sharedRewarder, DragonLookAction.Direction.YAW);
        this.sharedPitchAction = new DragonLookAction(sharedRewarder, DragonLookAction.Direction.PITCH);

        // Build shared context (suppliers read from evaluationState)
        var spartanContext = plugin.getSpartanApi().createContext("shared_dragon_context");
        var contextElements = buildSharedContextElements();
        contextElements.forEach(element ->
                spartanContext.addElement(element, contextElements.indexOf(element))
        );
        spartanContext.update();

        // Build shared action manager
        var actionManager = plugin.getSpartanApi().createActionManager();
        actionManager.registerAction(sharedYawAction);
        actionManager.registerAction(sharedPitchAction);

        // Build the model
        this.model = plugin.getSpartanApi()
                .createModel(
                        "spartan_shared_dragon_brain",
                        createDragonFlightConfig(mode),
                        spartanContext,
                        actionManager
                );
        model.register();

        // Attempt to load weights from disk
        if (model != null) {
            try {
                File spartanBaseDir = new File("ai", "model");
                if (!spartanBaseDir.exists()) {
                    spartanBaseDir.mkdirs();
                    logger.info("[DragonQueueManager] Created 'ai/model' directories in server root.");
                }

                new SpartanModelManager(model, "/dragon-model.spartan").loadModel();

                logger.info("[DragonQueueManager] Model weights saved successfully to server root: ai/model/dragon-model.spartan");
            } catch (Exception e) {
                logger.warning("[DragonQueueManager] Failed to save model: " + e.getMessage());
            }
        }

        logger.info("[DragonQueueManager] Shared brain ready. Strategy: " + strategy.label());
    }

    /**
     * Saves the model weights to disk, closes the model's native resources,
     * clears the queue, and resets the scratchpad.
     */
    public void shutdown() {
        logger.info("[DragonQueueManager] Shutting down. Total requests processed: "
                + totalProcessed.get() + ". Slow ticks: " + slowTickCount.get());

        // Session report, single model-level report
        modelMetrics.dumpSessionReport(plugin.getComponentLogger());

        // save
        if (model != null) {
            try {
                File spartanBaseDir = new File("ai", "model");
                if (!spartanBaseDir.exists()) {
                    spartanBaseDir.mkdirs();
                    logger.info("[DragonQueueManager] Created 'ai/model' directories in server root.");
                }

                new SpartanModelManager(model, "/dragon-model.spartan").saveModel();

                logger.info("[DragonQueueManager] Model weights saved successfully to server root: ai/model/dragon-model.spartan");
            } catch (Exception e) {
                logger.warning("[DragonQueueManager] Failed to save model: " + e.getMessage());
            }
        }

        // close model, release resources
        if (model != null) {
            try {
                model.close();
            } catch (Exception e) {
                logger.warning("[DragonQueueManager] Exception while closing model: " + e.getMessage());
            }
        }

        // unbound objects
        model = null;
        sharedYawAction = null;
        sharedPitchAction = null;

        //discard stale queue
        requestQueue.clear();
        evaluationState.reset();
    }


    /**
     * Enqueues a sensory snapshot for processing.
     *
     * @param request An immutable snapshot of the dragon's current state.
     *                Silently dropped if the manager has not been initialized
     *                or has been shut down.
     */
    public void submit(@NotNull DragonStateRequest request) {
        if (model == null) return; // not yet initialized or already shut down
        requestQueue.offer(request);
    }

    /**
     * Drains the entire request queue and processes every pending dragon in one
     * server tick (Option A — synchronous full-drain on the main thread).
     *
     * <h4>Backlog prevention guarantee</h4>
     * Because we drain the full queue before returning, the queue size at the end
     * of every tick is 0. The next tick can produce at most N new requests.
     * Queue size is therefore bounded by N at all times (it never accumulates).
     *
     *
     */
    public void processTick() {
        if (model == null) return;

        long tickStart = System.currentTimeMillis();
        int processed  = 0;

        // drain loop
        // new requests submitted by dragons during this loop (same tick, same
        // main thread) cannot arrive here because Minecraft's main thread is
        // single-threaded, controller.tick() and processTick() are serialized
        // by the game loop. There is no race condition
        DragonStateRequest request;
        while ((request = requestQueue.poll()) != null) {

            evaluationState.writeFrom(request);

            evaluationState.currentReward = request.requester().reward();

            // model.tick() reads from the context suppliers -> reads from
            // evaluationState -> sees dragon N's data. Writes outputs to
            // the ActionOutputBuffer, which is owned by sharedYawAction
            // and sharedPitchAction.
            strategy.execute(model, evaluationState);

            // getAngle() returns the raw [-1.0, 1.0] Tanh-squashed output
            // from the actor network, before scaling to degrees.
            // Scaling happens inside applyCalculatedActions(), preserving
            // the same semantics as the original monolithic controller
            if(sharedPitchAction == null || sharedYawAction == null) {
                logger.warning("[DragonQueueManager] Shared actions are not initialized. Skipping request processing.");
                continue;
            }

            double yawDelta   = sharedYawAction.getAngle();
            double pitchDelta = sharedPitchAction.getAngle();

            request.requester().applyCalculatedActions(yawDelta, pitchDelta);

            processed++;
        }

        if (processed > 0) {
            totalProcessed.addAndGet(processed);

            long elapsed = System.currentTimeMillis() - tickStart;
            if (elapsed > PROCESSING_WARN_THRESHOLD_MS) {
                slowTickCount.incrementAndGet();
                logger.warning(String.format(
                        "[DragonQueueManager] slow tick: processed %d requests in %dms " +
                                "(threshold: %dms)",
                        processed, elapsed, PROCESSING_WARN_THRESHOLD_MS
                ));
            }
        }
    }


    /**
     * Builds the list of {@link SpartanSingleContextElement}s whose suppliers
     * close over {@link #evaluationState} — <strong>not</strong> over any entity.
     *
     * @return Ordered list of context elements, indices matching those in the
     *         original {@code buildContextElements()} call.
     */
    @NotNull
    private List<SpartanSingleContextElement> buildSharedContextElements() {
        var list = new ArrayList<SpartanSingleContextElement>();

        list.add(createContextElement("target_dir_x", () -> evaluationState.targetDirX));
        list.add(createContextElement("target_dir_y", () -> evaluationState.targetDirY));
        list.add(createContextElement("target_dir_z", () -> evaluationState.targetDirZ));

        list.add(createContextElement("vel_x", () -> evaluationState.velX));
        list.add(createContextElement("vel_y", () -> evaluationState.velY));
        list.add(createContextElement("vel_z", () -> evaluationState.velZ));


        list.add(createContextElement("obstacle_distance",
                () -> evaluationState.obstacleDistanceRatio));

        list.add(createContextElement("target_distance",
                () -> evaluationState.targetDistance));

        list.add(createContextElement("yaw_error",
                () -> evaluationState.yawError));

        list.add(createContextElement("pitch_error",
                () -> evaluationState.pitchError));

        return list;
    }

    /**
     * Constructs the RSAC configuration, toggling {@code isTraining} based on the
     * active {@link DragonBrainMode}.
     *
     * @param mode Determines whether {@code isTraining} is set to {@code true}
     *             ({@link DragonBrainMode#TRAINING}) or {@code false}
     *             ({@link DragonBrainMode#INFERENCE}).
     */
    @NotNull
    private RecurrentSoftActorCriticConfig createDragonFlightConfig(@NotNull DragonBrainMode mode) {
        return RecurrentSoftActorCriticConfig.builder()
                .learningRate(3.0E-4)
                .policyNetworkLearningRate(3.0E-4)
                .firstCriticLearningRate(3.0E-4)
                .secondCriticLearningRate(3.0E-4)
                .gamma(0.99)
                .truncatedBPTTDepth(8)
                .hiddenStateSize(128)
                .recurrentLayerDepth(1)
                .recurrentInputFeatureCount(64)
                .actorHiddenLayerCount(2)
                .actorHiddenLayerNeuronCount(256)
                .criticHiddenLayerCount(2)
                .criticHiddenLayerNeuronCount(256)
                .targetEntropy(-2.0)
                .alphaLearningRate(1.0E-4)
                .entropyTemperatureAlpha(0.2)
                .squashActionsWithTanh(1.0)
                .targetSmoothingCoefficient(0.005)
                .remorseTraceBufferCapacity(4096)
                .remorseMinimumSimilarityThreshold(0.35)
                // The single gate that differs between training and inference:
                .isTraining(mode == DragonBrainMode.TRAINING)
                .build();
    }


    @NotNull
    private SpartanSingleContextElement createContextElement(
            @NotNull String id,
            @NotNull DoubleSupplier valueSupplier
    ) {
        return new SpartanSingleContextElement() {
            @Override
            public double getValue() {
                return valueSupplier.getAsDouble();
            }

            @Override
            public int getSize() { return 1; }

            @Override
            public void tick() { /* state is pushed by writeFrom(), not pulled here */ }

            @Override
            public @NotNull String getIdentifier() { return "dragon_ctx_" + id; }
        };
    }

    /** @return The session-level model metrics aggregator. Used by helpers at construction time. */
    public @NotNull DragonModelMetrics getModelMetrics() { return modelMetrics; }

    /** @return Number of requests currently waiting in the queue. Should be ~0 between ticks. */
    public int getQueueDepth() { return requestQueue.size(); }

    /** @return Total requests processed since initialization. */
    public int getTotalProcessed() { return totalProcessed.get(); }

    /** @return Number of ticks that exceeded {@link #PROCESSING_WARN_THRESHOLD_MS}. */
    public int getSlowTickCount() { return slowTickCount.get(); }

    /** @return The active {@link DragonBrainMode}. */
    public @NotNull DragonBrainMode getMode() { return mode; }

    /** @return {@code true} if the model has been initialized and is ready to process requests. */
    public boolean isReady() { return model != null; }
}