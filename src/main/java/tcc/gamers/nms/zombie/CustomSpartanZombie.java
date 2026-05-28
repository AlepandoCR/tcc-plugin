package tcc.gamers.nms.zombie;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.spartan.api.engine.SpartanModel;
import org.spartan.api.engine.action.SpartanActionManager;
import org.spartan.api.engine.config.SpartanModelConfig;
import org.spartan.api.engine.context.SpartanContext;
import org.spartan.api.exception.SpartanNativeException;
import org.spartan.api.exception.SpartanPersistenceException;
import tcc.gamers.TCCPlugin;
import tcc.gamers.config.ZombieConfig;
import tcc.gamers.ai.minecraft.SpartanBrainTickerBehaviorControl;
import tcc.gamers.ai.minecraft.TickBrainGoal;
import tcc.gamers.ai.spartan.monitor.SpartanActionMonitor;
import tcc.gamers.ai.spartan.model.SpartanModelManager;
import tcc.gamers.tutorials.model.Model;
import tcc.gamers.util.StorageFolder;

import java.nio.file.Paths;
import java.util.Set;

public class CustomSpartanZombie<SpartanModelConfigType extends SpartanModelConfig> extends Zombie {

    private final SpartanModelConfigType modelConfig;
    private final TCCPlugin plugin;
    private final SpartanActionMonitor actionMonitor;
    private final ZombieConfig zombieConfig;
    private Model<? extends Entity> visualModel;

    public CustomSpartanZombie(
            @NotNull Level level,
            @NotNull ZombieConfig zombieConfig,
            @NotNull SpartanModelConfigType modelConfig,
            @NotNull TCCPlugin plugin
    ) {
        super(EntityType.ZOMBIE, level);
        this.zombieConfig = zombieConfig;
        this.modelConfig = modelConfig;
        this.plugin = plugin;
        this.actionMonitor = new SpartanActionMonitor(plugin.getLogger(), "zombie " + this.getUUID());

        this.goalSelector.removeAllGoals((_) -> true);
        this.registerGoals();
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

        craftWorld.getHandle().addFreshEntity(this);
        this.getBukkitEntity().teleport(location);
        this.setSilent(true);
        this.setInvulnerable(true);
    }

    @Override
    public void registerGoals() {
        brain.removeAllBehaviors();
        goalSelector.removeAllGoals((_) -> true);

        if (modelConfig == null) {
            return;
        }

        SpartanContext spartanContext = buildSpartanContext();
        SpartanActionManager spartanActionManager = buildActionManager();
        Brain<CustomSpartanZombie<SpartanModelConfigType>> typedBrain = getTypedBrain();

        try {
            var spartanModel = setupSpartanModel(spartanContext, spartanActionManager);
            createBetterModel();

            var modelSavePath = Paths.get(plugin.getDataFolder().getAbsolutePath())
                    .resolve(StorageFolder.SPARTAN_MODEL.getFolderName())
                    .resolve("zombiemodel.spartan");

            var modelTickBehavior = new SpartanBrainTickerBehaviorControl<>(
                    spartanModel,
                    1,
                    100,
                    modelSavePath
            );

            typedBrain.addActivity(
                    Activity.CORE,
                    ImmutableList.of(
                            Pair.of(0, modelTickBehavior)
                    ),
                    Set.of(),
                    Set.of()
            );
        } catch (SpartanNativeException e) {
            plugin.getLogger().warning("Skipping Spartan CRSAC model for zombie " + this.getUUID() + ": " + e.getMessage());
            // abort model creation
        }

        this.goalSelector.addGoal(
                0,
                new TickBrainGoal<>(
                this, (_) -> {}
                )
        );
    }

    private @NonNull SpartanModel<SpartanModelConfigType> setupSpartanModel(
            @NotNull SpartanContext spartanContext,
            @NotNull SpartanActionManager spartanActionManager
    ) {
        var spartanModel = plugin
                .getSpartanApi()
                .createModel(
                        "zombie_model_" + this.getUUID(),
                        modelConfig,
                        spartanContext,
                        spartanActionManager
                );

        spartanModel.register();
        loadSpartanModel(spartanModel);

        return spartanModel;
    }

    private @NonNull SpartanContext buildSpartanContext() {
        SpartanContext spartanContext = plugin
                .getSpartanApi()
                .createContext("zombie_context_" + this.getUUID());

        //todo fill context

        spartanContext.update();
        return spartanContext;
    }

    private @NotNull SpartanActionManager buildActionManager() {
        return plugin.getSpartanApi().createActionManager();
    }

    private static <SpartanModelConfigType extends SpartanModelConfig> void loadSpartanModel(SpartanModel<SpartanModelConfigType> spartanModel) {
        var modelManager = new SpartanModelManager(spartanModel);
        try {
            modelManager.loadModel();
        } catch (SpartanPersistenceException e) {

        }
    }

    private void createBetterModel() {
        try {
            if (!this.zombieConfig.getModelName().isBlank()) {
                this.visualModel = Model.attachTo(this.zombieConfig.getModelName(), this.getBukkitEntity());
                this.visualModel.scale(this.zombieConfig.getModelScale());
            }
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Model '" + this.zombieConfig.getModelName() + "' not found or failed to attach: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull Brain<CustomSpartanZombie<SpartanModelConfigType>> getTypedBrain() {
        return (Brain<CustomSpartanZombie<SpartanModelConfigType>>) this.getBrain();
    }

    public void despawnZombie() {
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
}