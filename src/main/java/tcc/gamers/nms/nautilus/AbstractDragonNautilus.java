package tcc.gamers.nms.nautilus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.ai.minecraft.TickBrainGoal;
import tcc.gamers.tutorials.model.Model;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Base class for all dragon-type Nautilus entities.
 *
 * <p>Owns all physics math (thrust, dive energy, dash, stall), visual animation
 * dispatch, particle effects, and the Brain/GoalSelector registration API.
 * It is completely agnostic about <em>who or what</em> supplies the look vector;
 * that responsibility is delegated to subclasses via the abstract controller hook
 * {@link #tickController()}.</p>
 * */
public abstract class AbstractDragonNautilus<E extends AbstractNautilus> extends Nautilus {


    private static final @Nullable Field DASH_COOLDOWN_FIELD;

    static {
        Field f = null;
        try {
            f = AbstractNautilus.class.getDeclaredField("dashCooldown");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        DASH_COOLDOWN_FIELD = f;
    }

    private static final int DASH_DURATION_TICKS = 60;
    private static final double DASH_DECAY = 0.990;
    private static final int MAX_DASH_COMBO = 20;
    private static final double COMBO_BONUS_PER_STACK = 0.30;
    private static final float PITCH_DEAD_ZONE = 5.0F;
    private static final float STALL_PITCH_THRESHOLD = -15.0F;
    private static final int STALL_TICK_THRESHOLD = 10;
    private static final double MAX_DIVE_ENERGY = 3.0;
    private static final double IDLE_COMBO_DECAY_TICKS = 20;

    protected final @NotNull World world;
    protected final @NotNull TCCPlugin plugin;
    protected final @NotNull NautilusAnimationManager animationManager;

    private double currentForwardSpeed = 0.0;
    private double storedDiveEnergy = 0.0;
    private int stallTicks = 0;

    private Vec3 smoothedLookAngle = Vec3.ZERO;
    private float previousYRot = 0.0F;

    private int dashTicksRemaining = 0;
    private Vec3 dashVector = Vec3.ZERO;

    private int dashCombo = 0;
    private int idleTicks = 0;

    private double baseMovementSpeed  = -1.0;
    private boolean speedReducedForAI  = false;

    private boolean isAutonomous = false;

    private boolean freeze = false;

    private @Nullable Model<org.bukkit.entity.Nautilus> model;

    protected AbstractDragonNautilus(@NotNull World world, @NotNull TCCPlugin plugin) {
        super(EntityType.NAUTILUS, ((CraftWorld) world).getHandle());
        this.world  = world;
        this.plugin = plugin;

        this.getAttributes().registerAttribute(Attributes.FLYING_SPEED);
        var flyAttr = this.getAttribute(Attributes.FLYING_SPEED);
        if (flyAttr != null) flyAttr.setBaseValue(0.01);

        var movAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movAttr != null) this.baseMovementSpeed = movAttr.getBaseValue();

        this.animationManager = new NautilusAnimationManager();
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public void setIsAutonomous(boolean autonomous){
        this.isAutonomous = autonomous;
    }

    public boolean isAutonomous(){
        return this.isAutonomous;
    }

    public @NotNull Location getLocation(){
        return getBukkitNautilus().getLocation();
    }

    /**
     * Adds the entity to the world, attaches the geo model, and applies the
     * standard saddle + PersistentData tag.
     */
    public void spawn(@NotNull Location location) {
        var bukkit = this.getBukkitNautilus();
        world.addEntity(bukkit);

        this.setupBrain();

        this.model = Model.attachTo("fairy_dragon", bukkit);
        this.animationManager.attachModel(this.model);

        bukkit.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        bukkit.setTamed(true);
        bukkit.getPersistentDataContainer().set(
                TCCNameSpacedKeys.FLYING_NAUTILUS.getNamespacedKey(),
                PersistentDataType.BYTE, (byte) 1);
        bukkit.teleport(location);

        onSpawn(location);
    }

    abstract void onSpawn(@NotNull Location spawnLocation);


    public boolean isFreeze(){
        return this.freeze;
    }

    public void setFreeze(boolean freeze){
        this.freeze = freeze;
    }



    /**
     * Override to add {@link BehaviorControl}s to {@link Activity} buckets.
     * @param brain the entity's live brain instance (unchecked generic)
     */
    abstract void registerBrainActivities(@NotNull Brain<E> brain);

    abstract void registerExtraGoals(@NotNull GoalSelector goalSelector);

    @Override
    public void registerGoals() {
        goalSelector.removeAllGoals((_) -> true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupBrain() {
        Brain brain = this.brain;

        registerBrainActivities((Brain<E>) brain);

        this.goalSelector.addGoal(0, new TickBrainGoal<>(this, (_ -> {})));

        registerExtraGoals(this.goalSelector);
    }

    /**
     * Called every tick to let the subclass drive the entity.
     *
     * <p>Subclasses should call {@link #applyFlight(Vec3, float)} or
     * {@link #tickDash()} from here depending on their control source
     * (player look vector, PPO output, etc.).</p>
     *
     * <p>If the entity is riding a passenger this is only called when
     * {@link #isVehicle()} is true. If the entity is autonomous it's always
     * called.</p>
     */
    protected abstract void tickController();

    /**
     * Return {@code true} if this dragon should be removed this tick.
     * The base implementation always returns {@code false}.
     */
    protected boolean shouldDespawn() {
        return false;
    }


    @Override
    public void tick() {
        if (shouldDespawn()) {
            getModel().ifPresent(Model::despawn);
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        super.tick();

        suppressDashCooldown();
        tickIdleAndComboDecay();

        this.resetFallDistance();
        this.setAirSupply(this.getMaxAirSupply());

        if (this.isVehicle() || isAutonomous) {
            this.setNoGravity(true);
            restoreSpeedIfNeeded();
            capturePreviousRotation();

            // Delegate actual movement to the subclass
            tickController();

            updateMovementAnimations();
            this.needsSync = true;
        } else {
            reduceSpeedForAI();
        }
    }

    private void restoreSpeedIfNeeded() {
        if (speedReducedForAI) {
            var movAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movAttr != null && baseMovementSpeed > 0) {
                movAttr.setBaseValue(baseMovementSpeed);
            }
            speedReducedForAI = false;
        }
    }

    private void capturePreviousRotation() {
        this.previousYRot = this.getYRot();
    }

    private void reduceSpeedForAI() {
        var movAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movAttr != null && baseMovementSpeed > 0 && !speedReducedForAI) {
            movAttr.setBaseValue(baseMovementSpeed / 4.0);
            speedReducedForAI = true;
        }
    }

    /**
     * Applies one tick of flight physics driven by an external look vector.
     *
     * <p>This is the primary entry point that subclasses call from
     * {@link #tickController()}.  The look vector can come from a player,
     * a PPO network, a scripted path follower — anything.</p>
     *
     * @param lookAngle the unit look direction (world-space)
     * @param pitch     the vertical pitch in degrees (positive = looking down)
     */
    public void applyFlight(@NotNull Vec3 lookAngle, float pitch) {
        if(freeze) return;
        if (this.onGround()) {
            this.storedDiveEnergy    = 0.0;
            this.stallTicks          = 0;
            this.currentForwardSpeed = 0.0;
        }

        double comboMultiplier = 1.0 + (this.dashCombo * COMBO_BONUS_PER_STACK);
        double baseThrust = (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.15) * comboMultiplier;
        double targetSpeed;

        if (pitch > PITCH_DEAD_ZONE) {
            targetSpeed = this.handleDownInclination(pitch, baseThrust);
        } else if (pitch < -PITCH_DEAD_ZONE) {
            targetSpeed = this.handleUpInclination(pitch, baseThrust, baseThrust);
        } else {
            float blendFactor     = Math.abs(pitch) / PITCH_DEAD_ZONE;
            double baseLevelFlight = baseThrust * 1.5;

            if (pitch > 0) {
                double diveTarget = this.handleDownInclination(pitch, baseLevelFlight);
                targetSpeed = Mth.lerp(blendFactor, baseLevelFlight, diveTarget);
            } else if (pitch < 0) {
                double climbTarget = this.handleUpInclination(pitch, baseLevelFlight, baseThrust);
                targetSpeed = Mth.lerp(blendFactor, baseLevelFlight, climbTarget);
            } else {
                targetSpeed = baseLevelFlight;
            }
            this.handleLevelFlight();
        }

        double lerpRate = (targetSpeed < this.currentForwardSpeed) ? 0.02 : 0.15;
        this.currentForwardSpeed = Mth.lerp(lerpRate, this.currentForwardSpeed, targetSpeed);

        this.smoothedLookAngle = lerpVec3(0.15, this.smoothedLookAngle, lookAngle);

        Vec3 newMotion = this.calculateFlightMotion(pitch, this.smoothedLookAngle, baseThrust);
        this.setDeltaMovement(newMotion);
        this.move(MoverType.SELF, this.getDeltaMovement());
        spawnBubbles();
    }

    /**
     * Advances one tick of an active dash.  Call from {@link #tickController()}
     * when {@link #isDashing()} returns {@code true}.
     */
    protected void tickDash() {
        this.dashTicksRemaining--;
        suppressDashCooldownOneTick();

        this.dashVector        = this.dashVector.scale(DASH_DECAY);
        this.currentForwardSpeed = this.dashVector.length();

        Vec3 dashDir = this.currentForwardSpeed > 1e-6
                ? this.dashVector.normalize()
                : this.smoothedLookAngle;
        this.smoothedLookAngle = lerpVec3(0.3, this.smoothedLookAngle, dashDir);

        this.setDeltaMovement(this.dashVector);
        this.move(MoverType.SELF, this.getDeltaMovement());
        spawnBubbles();
    }

    /**
     * Initiates a dash in the given direction with the given jump scale.
     *
     * @param lookDir   the direction to dash toward
     * @param jumpScale jump button pressure [0.0 – 1.0]
     */
    protected void initiateDash(@NotNull Vec3 lookDir, float jumpScale) {
        if (this.dashTicksRemaining > DASH_DURATION_TICKS - 10) return;

        this.dashCombo = Math.min(this.dashCombo + 1, MAX_DASH_COMBO);
        this.idleTicks = 0;

        float  effectiveAmount  = Math.max(jumpScale, 0.5F);
        double comboMultiplier  = 1.0 + (this.dashCombo * COMBO_BONUS_PER_STACK);
        double dashBoost        = ((this.isInWater() ? 1.2F : 0.5F) * effectiveAmount)
                * (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * comboMultiplier)
                * this.getBlockSpeedFactor();

        this.stallTicks          = 0;
        double baseDashSpeed     = dashBoost * 4.5;
        double targetSpeed       = this.currentForwardSpeed + baseDashSpeed;
        double maxAllowedSpeed   = 5.0 + (this.dashCombo * 0.8);
        double finalDashSpeed    = Math.min(targetSpeed, maxAllowedSpeed);

        this.dashVector          = lookDir.scale(finalDashSpeed);
        this.dashTicksRemaining  = DASH_DURATION_TICKS;
        this.smoothedLookAngle   = lookDir;
        this.currentForwardSpeed = finalDashSpeed;
        this.storedDiveEnergy    = Math.min(this.storedDiveEnergy + (dashBoost * 1.5), MAX_DIVE_ENERGY);

        setDashCooldownField();
        animationManager.play(NautilusAnimationManager.Animation.BELLY);
        this.setDashing(true);
        this.needsSync = true;
    }


    private double handleDownInclination(float pitch, double currentTargetSpeed) {
        if (this.onGround()) return currentTargetSpeed;
        double diveMultiplier = pitch / 90.0;
        this.storedDiveEnergy = Math.min(this.storedDiveEnergy + (0.08 * diveMultiplier), MAX_DIVE_ENERGY);
        return currentTargetSpeed + this.storedDiveEnergy;
    }

    private double handleUpInclination(float pitch, double currentTargetSpeed, double baseThrust) {
        double climbMultiplier = Math.abs(pitch) / 90.0;
        if (this.storedDiveEnergy > 0) {
            this.storedDiveEnergy = Math.max(this.storedDiveEnergy - (0.04 * climbMultiplier), 0.0);
            return currentTargetSpeed + (this.storedDiveEnergy * 1.2);
        } else {
            double penalized = currentTargetSpeed - (baseThrust * 0.15 * climbMultiplier);
            return Math.max(penalized, baseThrust * 0.7);
        }
    }

    private void handleLevelFlight() {
        this.storedDiveEnergy = Math.max(this.storedDiveEnergy - 0.005, 0.0);
    }

    private Vec3 calculateFlightMotion(float pitch, Vec3 lookAngle, double baseThrust) {
        if (this.currentForwardSpeed < 0.05 && pitch < STALL_PITCH_THRESHOLD && this.storedDiveEnergy < 0.2) {
            this.stallTicks++;
        } else {
            this.stallTicks = Math.max(this.stallTicks - 2, 0);
        }

        if (this.stallTicks > STALL_TICK_THRESHOLD) {
            Vec3 stallFall = this.getDeltaMovement().add(0, -0.15, 0).scale(0.9);
            this.currentForwardSpeed *= 0.8;
            this.storedDiveEnergy     = 0.0;
            return stallFall;
        }

        Vec3 controlled = lookAngle.scale(this.currentForwardSpeed);
        if (this.currentForwardSpeed < baseThrust && pitch <= 5.0F && !this.onGround()) {
            controlled = controlled.add(0, -0.01, 0);
        }
        if (pitch < -5.0F && this.currentForwardSpeed > 0.1) {
            controlled = controlled.add(0, 0.03, 0);
        }
        return controlled;
    }


    private void updateMovementAnimations() {
        getModel().ifPresent(m -> m.rotate(this.getXRot(), this.getYRot()));

        if (this.currentForwardSpeed < 0.1 && this.dashTicksRemaining <= 0) return;

        if (this.onGround()) {
            animationManager.play(NautilusAnimationManager.Animation.WALK_GROUND);
        } else {
            float turnDelta = Mth.wrapDegrees(this.getYRot() - this.previousYRot);
            float threshold = 3.5F;

            if (turnDelta > threshold) {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK_RIGHT);
            } else if (turnDelta < -threshold) {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK_LEFT);
            } else {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK);
            }
        }
    }

    private void tickIdleAndComboDecay() {
        if (this.currentForwardSpeed < 0.1 && this.dashTicksRemaining <= 0) {
            if (onGround()) {
                animationManager.play(NautilusAnimationManager.Animation.IDLE_GROUND);
            } else {
                animationManager.play(NautilusAnimationManager.Animation.FLY_IDLE);
            }

            this.idleTicks++;
            if (this.idleTicks >= IDLE_COMBO_DECAY_TICKS) {
                if (this.dashCombo > 0) this.dashCombo--;
                this.idleTicks = 0;
            }
        } else {
            this.idleTicks = 0;
        }
    }

    private void spawnBubbles() {
        double speed = this.getDeltaMovement().length();
        double prob  = Mth.clamp(speed * 2.0, 0.15, 1.0);
        if (this.random.nextFloat() < prob) {
            float yRot   = this.getYRot();
            float xRot   = Mth.clamp(this.getXRot(), -10.0F, 10.0F);
            Vec3  mouth  = this.calculateViewVector(xRot, yRot);
            double spread = this.random.nextDouble() * 0.8 * (1.0 + speed);
            double dx = (this.random.nextFloat() - 0.5) * spread;
            double dy = (this.random.nextFloat() - 0.5) * spread;
            double dz = (this.random.nextFloat() - 0.5) * spread;
            this.level().addParticle(ParticleTypes.BUBBLE,
                    this.getX() - mouth.x * 1.1,
                    this.getY() - mouth.y + 0.25,
                    this.getZ() - mouth.z * 1.1,
                    dx, dy, dz);
        }
    }


    @Override
    public int getJumpCooldown() {
        return 0;
    }

    @NotNull
    @Override
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        if (this.isVehicle()) return; // All movement handled in tickController().
        this.travelFlying(input, 1.0F);
        super.travel(input);
    }



    /** Suppresses the vanilla dash cooldown every tick so it never blocks us. */
    private void suppressDashCooldown() {
        try {
            if (DASH_COOLDOWN_FIELD != null) {
                DASH_COOLDOWN_FIELD.setInt(this, 0);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /** Decrements vanilla cooldown by 1 tick (called during an active dash). */
    private void suppressDashCooldownOneTick() {
        try {
            if (DASH_COOLDOWN_FIELD != null) {
                int current = DASH_COOLDOWN_FIELD.getInt(this);
                if (current > 0) DASH_COOLDOWN_FIELD.setInt(this, Math.max(current - 1, 0));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /** Sets the vanilla dash cooldown to 40 on dash initiation. */
    private void setDashCooldownField() {
        try {
            if (DASH_COOLDOWN_FIELD != null) DASH_COOLDOWN_FIELD.setInt(this, 40);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public double getCurrentForwardSpeed() { return currentForwardSpeed; }
    public double getStoredDiveEnergy()    { return storedDiveEnergy; }
    public int    getDashCombo()           { return dashCombo; }

    public @NotNull NautilusAnimationManager getAnimationManager() { return animationManager; }

    public @NotNull Optional<Model<org.bukkit.entity.Nautilus>> getModel() {
        return Optional.ofNullable(model);
    }

    public @NotNull org.bukkit.entity.Nautilus getBukkitNautilus() {
        return (org.bukkit.entity.Nautilus) getBukkitEntity();
    }

    public @NotNull org.bukkit.util.Vector getBukkitSpeed() {
        Vec3 dm = this.getDeltaMovement();
        return new org.bukkit.util.Vector(dm.x, dm.y, dm.z);
    }


    @NotNull
    protected static Vec3 lerpVec3(double t, @NotNull Vec3 a, @NotNull Vec3 b) {
        return new Vec3(
                Mth.lerp(t, a.x, b.x),
                Mth.lerp(t, a.y, b.y),
                Mth.lerp(t, a.z, b.z)
        );
    }
}