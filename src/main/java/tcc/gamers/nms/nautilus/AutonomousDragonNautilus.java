package tcc.gamers.nms.nautilus;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spartan.api.engine.config.RecurrentSoftActorCriticConfig;
import org.spartan.api.engine.model.RecurrentSoftActorCriticModel;
import org.spartan.api.exception.SpartanNativeException;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.controller.DragonLookController;
import tcc.gamers.ai.minecraft.SpartanBrainTickerBehaviorControl;
import tcc.gamers.util.StorageFolder;

import java.nio.file.Paths;
import java.util.Set;


public class AutonomousDragonNautilus extends AbstractDragonNautilus<AutonomousDragonNautilus> {

    private final @NotNull DragonLookController controller;

    private @Nullable RecurrentSoftActorCriticModel model;

    private @NotNull Location targetLocation;

    private boolean started = false;

    protected AutonomousDragonNautilus(@NotNull World world, @NotNull TCCPlugin plugin) {
        super(world, plugin);
        this.controller = new DragonLookController(this);
        this.targetLocation = this.getBukkitNautilus().getLocation(); //start self as target
    }

    @Override
    void registerBrainActivities(@NotNull Brain<AutonomousDragonNautilus> brain) {

        var modelSavePath = Paths.get(
                        plugin.getDataFolder().getAbsolutePath()
                )
                .resolve(
                        StorageFolder.SPARTAN_MODEL.getFolderName()
                )
                .resolve("dragon-model.spartan");

        try {
            this.model = buildModel();
            brain.addActivity(
                    Activity.CORE,
                    ImmutableList.of(
                            Pair.of(1, new SpartanBrainTickerBehaviorControl<>(model, 1, 200, modelSavePath))
                    ),
                    Set.of(),
                    Set.of()
             );
        }catch(SpartanNativeException exception){
            plugin.getLogger().warning("Spartan Native Exception: " + exception.getMessage());
        }
    }

    @Override
    void registerExtraGoals(@NotNull GoalSelector goalSelector) {

    }

    public void start(){
        this.started = true;
    }

    public void stop(){
        this.started = false;
    }

    public void setTargetLocation(@NotNull Location location){
        this.targetLocation = location;
    }

    public @NotNull Location getLocationTarget(){
        return targetLocation;
    }

    public @NotNull Vec3 getTargetOffset() {
        Vec3 targetVec = new Vec3(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ());
        return targetVec.subtract(this.position());
    }

    public boolean hasArrived(){
        return controller.hasReachedTarget();
    }

    public boolean hasTarget() {
        return !targetLocation.equals(this.getBukkitNautilus().getLocation());
    }

    @Override
    protected void tickController() {
        if(started) controller.tick();
    }

    @Contract(" -> new")
    private @NotNull RecurrentSoftActorCriticModel buildModel(){
        var spartanContext = plugin.getSpartanApi().createContext("dragon_context");
        var contextElements = this.controller.buildContextElements();
        contextElements.forEach(
                element -> spartanContext.addElement(
                        element,
                        contextElements.indexOf(
                                element
                        )
                )
        );

        spartanContext.update();

        var actionManager = plugin.getSpartanApi().createActionManager();

        this.controller.getActions().forEach(actionManager::registerAction);

        return (RecurrentSoftActorCriticModel)
                plugin.getSpartanApi()
                        .createModel(
                                "spartan-dragon-model-for-entity-" + this.getStringUUID(),
                                createDragonFlightConfig(),
                                spartanContext,
                                actionManager
                        );
    }

    /**
     * Constructs the RSAC configuration optimized for high-speed, continuous 3D spatial navigation.
     * The model is tuned to prioritize long-term trajectory alignment (Point A to Point B)
     * over immediate, short-sighted movements.
     */
    @NotNull
    @Contract(" -> new")
    private RecurrentSoftActorCriticConfig createDragonFlightConfig() {
        return RecurrentSoftActorCriticConfig.builder()
                // core learning rates
                // Keeping learning rates standard for SAC. Too high will cause catastrophic
                // forgetting in continuous spaces; too low will make convergence painfully slow
                .learningRate(3.0E-4)
                .policyNetworkLearningRate(3.0E-4)
                .firstCriticLearningRate(3.0E-4)
                .secondCriticLearningRate(3.0E-4)

                // temporal and recurrency settings
                // gamma 0.99 ensures the AI values the long-term reward of reaching the target
                .gamma(0.99)
                // BPTT Depth set to 8. This allows the recurrent network to backpropagate through
                // the last 8 ticks, giving the AI a physical "sense" of its own momentum and acceleration.
                .truncatedBPTTDepth(8)
                .hiddenStateSize(128)
                .recurrentLayerDepth(1)
                .recurrentInputFeatureCount(64)

                // network arch
                // 256x2 standard
                .actorHiddenLayerCount(2)
                .actorHiddenLayerNeuronCount(256)
                .criticHiddenLayerCount(2)
                .criticHiddenLayerNeuronCount(256)

                // entropy and exploration!
                // target entropy typically equals -dim(A), we have 2 actions (yaw, pitch)
                .targetEntropy(-2.0)
                // alpha LR allows the model to auto-tune its entropy (exploration rate)
                // it starts exploring aggressively and stabilizes as it learns the flight path ideally
                .alphaLearningRate(1.0E-4)
                .entropyTemperatureAlpha(0.2)

                // action bounding
                // extremely important: Squashes network outputs strictly between [-1.0, 1.0] using Tanh
                .squashActionsWithTanh(1.0)

                // Polyak averaging for stable target network updates
                .targetSmoothingCoefficient(0.005)

                // buffer and memory
                .remorseTraceBufferCapacity(2000)
                .remorseMinimumSimilarityThreshold(0.75)

                .isTraining(true) // always training - Spartan signature
                .build();
    }
}