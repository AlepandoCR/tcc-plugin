package tcc.gamers.nms.horses;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spartan.api.engine.SpartanModel;
import org.spartan.api.engine.action.SpartanActionManager;
import org.spartan.api.engine.config.SpartanModelConfig;
import org.spartan.api.engine.context.SpartanContext;
import org.spartan.api.exception.SpartanNativeException;
import org.spartan.api.exception.SpartanPersistenceException;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.spartan.context.PointInclinationContextElement;
import tcc.gamers.config.HorseConfig;
import tcc.gamers.ai.controller.SpeedController;
import tcc.gamers.ai.minecraft.FollowPathGoal;
import tcc.gamers.ai.minecraft.SpartanBrainTickerBehaviorControl;
import tcc.gamers.ai.minecraft.TickBrainGoal;
import tcc.gamers.ai.spartan.action.ChangeSpeedAction;
import tcc.gamers.ai.spartan.context.PointDistanceContextElement;
import tcc.gamers.ai.spartan.monitor.SpartanActionMonitor;
import tcc.gamers.ai.spartan.model.SpartanModelManager;
import tcc.gamers.tutorials.model.Model;
import tcc.gamers.util.path.Path;
import tcc.gamers.util.path.PathMode;
import tcc.gamers.util.StorageFolder;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

public class CustomSpartanHorse<SpartanModelConfigType extends SpartanModelConfig> extends Horse {

    private final Path path;

    private final PathMode pathMode;

    private final SpeedController speedController;

    private final SpartanModelConfigType modelConfig;

    private final TCCPlugin plugin;

    private final SpartanActionMonitor actionMonitor;

    private final HorseConfig horseConfig;

    private Model<? extends Entity> visualModel;

    public CustomSpartanHorse(
            @NotNull Level level,
            @NotNull Path path,
            @NotNull PathMode pathMode,
            @NotNull HorseConfig horseConfig,
            @NotNull SpartanModelConfigType modelConfig,
            @NotNull TCCPlugin plugin
    ) {
        super(
                EntityType.HORSE,
                level
        );
        this.path = path;
        this.pathMode = pathMode;
        this.horseConfig = horseConfig;
        this.modelConfig = modelConfig;
        this.plugin = plugin;
        this.actionMonitor = new SpartanActionMonitor(plugin.getLogger(), "horse " + this.getUUID());
        // Use a modest max delta per tick to smooth abrupt speed transitions
        float minSpeed = Math.min((float)horseConfig.getMinSpeed(),(float)horseConfig.getMaxSpeed());
        float maxSpeed = Math.max((float)horseConfig.getMinSpeed(), (float)horseConfig.getMaxSpeed());
        float initialSpeed = minSpeed + ((maxSpeed - minSpeed) * 0.5f);
        this.speedController = new SpeedController(
                initialSpeed,
                maxSpeed,
                minSpeed,
                0.05f
        );

        // Re-initialize goals now that fields are set up
        this.goalSelector.removeAllGoals((_) -> true);
        this.registerGoals();

        this.setTamed(true);
    }


    @NotNull
    public SpartanActionMonitor.Snapshot getActionMonitorSnapshot() {
        return actionMonitor.snapshot();
    }


    @SuppressWarnings("unused")
    public void spawn(@NotNull Location location){
        var world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        var craftWorld = (CraftWorld) world;

        this.setPos(
                location.getX(),
                location.getY(),
                location.getZ()
        );
        this.setYRot(location.getYaw());
        this.setXRot(location.getPitch());

        craftWorld
                .getHandle()
                .addFreshEntity(this);

        this.getBukkitEntity().teleport(location);

        this.setSilent(true);

        this.setInvulnerable(true);

    }

    @Override
    public void registerGoals() {
        brain.removeAllBehaviors();
        goalSelector.removeAllGoals((_) -> true);
        // Early return if fields are not yet initialized (called from super constructor)
        if (path == null || speedController == null || modelConfig == null) {
            return;
        }

        var followPathGoal = new FollowPathGoal<>(
                path,
                pathMode,
                speedController,
                plugin
        );

        var pointDistanceContextElement = new PointDistanceContextElement(
                () -> Optional.of(this.getBukkitEntity()),
                followPathGoal::getCurrentPoint
        );

        var pointInclinationContextElement = new PointInclinationContextElement(
                () -> Optional.of(this.getBukkitEntity()),
                followPathGoal::getCurrentPoint
        );

        SpartanContext spartanContext = buildSpartanContext(pointDistanceContextElement);

        var spartanActionManager = buildActionManager(
                pointDistanceContextElement,
                pointInclinationContextElement
        );

        Brain<CustomSpartanHorse<SpartanModelConfigType>> typedBrain = getTypedBrain();

        try {

            var spartanModel = setupSpartanModel(spartanContext, spartanActionManager);

            createBetterModel();

            // Create model save path for automatic persistence
            var modelSavePath = Paths.get(plugin.getDataFolder().getAbsolutePath())
                    .resolve(StorageFolder.SPARTAN_MODEL.getFolderName())
                    .resolve("horsemodel.spartan");

            var modelTickBehavior = new SpartanBrainTickerBehaviorControl<>(
                    spartanModel,
                    1,
                    100,
                    modelSavePath
            );

            typedBrain.addActivity(
                    Activity.CORE,
                    ImmutableList.of(
                            Pair.of(1, followPathGoal),
                            Pair.of(0, modelTickBehavior)
                    ),
                    Set.of(),
                    Set.of()
            );
        } catch (SpartanNativeException e) {
            plugin.getLogger().warning("Skipping Spartan CRSAC model for horse " + this.getUUID() + ": " + e.getMessage());

            typedBrain.addActivity(
                    Activity.CORE,
                    ImmutableList.of(
                            Pair.of(0, followPathGoal)
                    ),
                    Set.of(),
                    Set.of()
            );
        }
        this.goalSelector.addGoal(
                0,
                new TickBrainGoal<>(
                        this,
                        (tickCount) -> {

                            if (tickCount % horseConfig.getDespawnCheckPeriodTicks() == 0 && this.isFarFromAllPlayers(horseConfig.getDespawnDistance())) {
                                this.despawnHorse();
                                return;
                            }
                            try {
                                this.controllerTick();
                            } catch (Throwable ignored) {
                            }

                        })
        );
    }

    private @NotNull SpartanModel<SpartanModelConfigType> setupSpartanModel(
            @NotNull SpartanContext spartanContext,
            @NotNull SpartanActionManager spartanActionManager
    ) {
        var spartanModel = plugin
                .getSpartanApi()
                .createModel(
                "horse_model_" + this.getUUID(),
                modelConfig,
                        spartanContext,
                        spartanActionManager
        );

        spartanModel.register();

        loadTaxiModel(spartanModel);

        return spartanModel;
    }

    private @NotNull SpartanContext buildSpartanContext(
            @NotNull PointDistanceContextElement pointDistanceContextElement
    ) {
        SpartanContext spartanContext = plugin
                .getSpartanApi()
                .createContext(
                        "horse_context_" + this.getUUID()
        );


        spartanContext.addElement(pointDistanceContextElement, 0);
        pointDistanceContextElement.tick();


        spartanContext.update();
        return spartanContext;
    }

    private static <SpartanModelConfigType extends SpartanModelConfig> void loadTaxiModel(SpartanModel<SpartanModelConfigType> spartanModel) {
        // Load trained model if it exists
        var modelManager = new SpartanModelManager(spartanModel, "/horsemodel.spartan");
        try {
            modelManager.loadModel();
        } catch (SpartanPersistenceException e) {
            // Model doesn't exist yet, that's fine - it will be created
        }
    }

    private void createBetterModel() {
        // Attach BetterModel visual to the existing Bukkit entity (if available).
        try {
            if (!this.horseConfig.getModelName().isBlank()) {
                this.visualModel = Model.attachTo(this.horseConfig.getModelName(), this.getBukkitEntity());
                this.visualModel.scale(this.horseConfig.getModelScale());
            }
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Model '" + this.horseConfig.getModelName() + "' not found or failed to attach: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull Brain<CustomSpartanHorse<SpartanModelConfigType>> getTypedBrain() {
        return (Brain<CustomSpartanHorse<SpartanModelConfigType>> ) this.getBrain();
    }

    /** Called once per server tick to advance internal controllers (e.g. speed smoothing). */
    @SuppressWarnings("unused")
    public void controllerTick() {
        if (this.speedController != null) {
            this.speedController.tick();
        }
    }

    public boolean isFarFromAllPlayers(double maxDistance) {
        var world = this.getBukkitEntity().getWorld();

        double maxDistanceSquared = maxDistance * maxDistance;
        var horseLocation = this.getBukkitEntity().getLocation();

        for (var player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }

            if (player.getLocation().distanceSquared(horseLocation) <= maxDistanceSquared) {
                return false;
            }
        }

        return true;
    }

    public void despawnHorse() {
        try {
            if (this.visualModel != null) {
                this.visualModel.despawn();
                this.visualModel = null;
            }
        } catch (Throwable ignored) {
        }

        try {
            this.getBukkitEntity().remove();
        } catch (Throwable ignored) {
        }
    }

    private @NotNull SpartanActionManager buildActionManager(
            @NotNull PointDistanceContextElement pointDistanceContextElement,
            @NotNull PointInclinationContextElement inclinationContextElement
    ) {
        var spartanActionManager = plugin.getSpartanApi().createActionManager();

        var speedAction = new ChangeSpeedAction(
                speedController,
                pointDistanceContextElement,
                inclinationContextElement,
                actionMonitor
        );

        spartanActionManager.registerAction(speedAction);
        return spartanActionManager;
    }
}
