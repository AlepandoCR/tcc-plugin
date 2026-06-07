package tcc.gamers.nms.nautilus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tcc.gamers.TCCPlugin;
import tcc.gamers.tutorials.model.Model;
import tcc.gamers.util.namespacedkey.TCCNameSpacedKeys;

import java.lang.reflect.Field;
import java.util.Optional;

public class CustomNautilus extends Nautilus {

    private static Field dashCooldownField;

    static {
        try {
            dashCooldownField = AbstractNautilus.class.getDeclaredField("dashCooldown");
            dashCooldownField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final @NotNull World world;
    private final @NotNull TCCPlugin plugin;
    private final NautilusAnimationManager animationManager;

    private double currentForwardSpeed = 0.0;
    private double storedDiveEnergy = 0.0;
    private int stallTicks = 0;

    private Vec3 smoothedLookAngle = Vec3.ZERO;
    private float previousYRot = 0.0F; // Guardamos la rotación anterior para calcular el giro

    private int dashTicksRemaining = 0;
    private Vec3 dashVector = Vec3.ZERO;

    private static final int DASH_DURATION_TICKS = 60;
    private static final double DASH_DECAY = 0.990;

    private int dashCombo = 0;
    private int idleTicks = 0;
    private static final int MAX_DASH_COMBO = 20;
    private static final double COMBO_BONUS_PER_STACK = 0.30;

    private final org.bukkit.entity.Player owner;

    private @Nullable Model<org.bukkit.entity.Nautilus> model;

    public CustomNautilus(@NotNull World level, @NotNull TCCPlugin plugin, @NotNull org.bukkit.entity.Player owner) {
        super(EntityType.NAUTILUS, ((CraftWorld) level).getHandle());
        this.world = level;
        this.plugin = plugin;
        this.owner = owner;
        this.animationManager = new NautilusAnimationManager();
    }

    public void spawn(@NotNull Location location) {
        var bukkitEntity = this.getBukkitNautilus();
        world.addEntity(bukkitEntity);
        var key = TCCNameSpacedKeys.FLYING_NAUTILUS.getNamespacedKey();
        setSpeed();

        this.model = Model.attachTo("fairy_dragon", bukkitEntity);
        this.animationManager.attachModel(this.model);

        bukkitEntity.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        bukkitEntity.setTamed(true);
        bukkitEntity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        bukkitEntity.teleport(location);
    }

    public @NotNull Optional<Model<org.bukkit.entity.Nautilus>> getModel(){
        return Optional.ofNullable(model);
    }

    private void setSpeed() {
        var attribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attribute != null){
            attribute.setBaseValue(attribute.getBaseValue() * 3.0);
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public int getJumpCooldown() {
        return 0;
    }

    @Override
    public void tick() {
        if(!owner.isValid() || !owner.isOnline() || owner.getWorld() != getBukkitNautilus().getWorld() || owner.getLocation().distance(getBukkitNautilus().getLocation()) > 15){
            getModel().ifPresent(Model::despawn);
            this.remove(RemovalReason.DISCARDED);
        }

        super.tick();

        try {
            if (dashCooldownField != null) {
                dashCooldownField.setInt(this, 0);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        isIdle();

        this.resetFallDistance();
        this.setAirSupply(this.getMaxAirSupply());

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            this.setNoGravity(true);

            this.previousYRot = this.getYRot();

            this.setYRot(player.getYRot());
            this.setXRot(player.getXRot());


            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.yRotO = this.previousYRot;

            if (this.dashTicksRemaining > 0) {
                this.tickDash();
            } else {
                this.applyFlight(player);
            }

            updateMovementAnimations();

            this.needsSync = true;
        } else {
            this.setNoGravity(false);
        }
    }

    private void updateMovementAnimations() {

        getModel().ifPresent(model -> model.rotate(this.getXRot(), this.getYRot()));

        if (this.currentForwardSpeed < 0.1 && this.dashTicksRemaining <= 0) {
            return;
        }

        if (this.onGround()) {
            animationManager.play(NautilusAnimationManager.Animation.WALK_GROUND);
        } else {

            float turnDelta = Mth.wrapDegrees(this.getYRot() - this.previousYRot);
            float sensitivityThreshold = 3.5F;

            if (turnDelta > sensitivityThreshold) {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK_RIGHT);
            } else if (turnDelta < -sensitivityThreshold) {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK_LEFT);
            } else {
                animationManager.play(NautilusAnimationManager.Animation.FLY_WALK);
            }
        }
    }

    private void isIdle() {
        if (this.currentForwardSpeed < 0.1 && this.dashTicksRemaining <= 0) {

            if (onGround()) {
                animationManager.play(NautilusAnimationManager.Animation.IDLE_GROUND);
            } else {
                animationManager.play(NautilusAnimationManager.Animation.FLY_IDLE);
            }

            this.idleTicks++;
            if (this.idleTicks >= 20) {
                if (this.dashCombo > 0) {
                    this.dashCombo--;
                }
                this.idleTicks = 0;
            }
        } else {
            this.idleTicks = 0;
        }
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            return;
        }
        this.travelFlying(input, 1.0F);
        super.travel(input);
    }

    @Override
    protected void tickRidden(@NotNull Player controller, @NotNull Vec3 riddenInput) {
        if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
            this.executeRidersJump(this.playerJumpPendingScale, controller);
        }
        this.playerJumpPendingScale = 0.0F;
    }

    private void tickDash() {
        this.dashTicksRemaining--;

        try {
            if (dashCooldownField != null) {
                int currentCooldown = dashCooldownField.getInt(this);
                if (currentCooldown > 0) {
                    dashCooldownField.setInt(this, Math.max(currentCooldown - 1, 0));
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        this.dashVector = this.dashVector.scale(DASH_DECAY);
        this.currentForwardSpeed = this.dashVector.length();

        Vec3 dashDir = this.currentForwardSpeed > 1e-6
                ? this.dashVector.normalize()
                : this.smoothedLookAngle;
        this.smoothedLookAngle = lerpVec3(0.3, this.smoothedLookAngle, dashDir);

        this.setDeltaMovement(this.dashVector);
        this.move(MoverType.SELF, this.getDeltaMovement());
        spawnBubbles();
    }

    private void applyFlight(@NotNull Player player) {
        if (this.onGround()) {
            this.storedDiveEnergy    = 0.0;
            this.stallTicks          = 0;
            this.currentForwardSpeed = 0.0;
        }

        Vec3  lookAngle = player.getLookAngle();
        float pitch     = player.getXRot();

        double comboMultiplier = 1.0 + (this.dashCombo * COMBO_BONUS_PER_STACK);
        double baseThrust = (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.15) * comboMultiplier;
        double targetSpeed;

        float pitchDeadzone = 5.0F;

        if (pitch > pitchDeadzone) {
            targetSpeed = this.handleDownInclination(pitch, baseThrust);
        } else if (pitch < -pitchDeadzone) {
            targetSpeed = this.handleUpInclination(pitch, baseThrust, baseThrust);
        } else {
            float blendFactor = Math.abs(pitch) / pitchDeadzone;
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

    @NotNull
    @Contract("_, _, _ -> new")
    private static Vec3 lerpVec3(double t, @NotNull Vec3 a, @NotNull Vec3 b) {
        return new Vec3(
                Mth.lerp(t, a.x, b.x),
                Mth.lerp(t, a.y, b.y),
                Mth.lerp(t, a.z, b.z)
        );
    }

    private double handleDownInclination(float pitch, double currentTargetSpeed) {
        if (this.onGround()) return currentTargetSpeed;
        double diveMultiplier = pitch / 90.0;
        this.storedDiveEnergy = Math.min(this.storedDiveEnergy + (0.08 * diveMultiplier), 2.5);
        return currentTargetSpeed + this.storedDiveEnergy;
    }

    private double handleUpInclination(float pitch, double currentTargetSpeed, double baseThrust) {
        double climbMultiplier = Math.abs(pitch) / 90.0;

        if (this.storedDiveEnergy > 0) {
            this.storedDiveEnergy = Math.max(this.storedDiveEnergy - (0.04 * climbMultiplier), 0.0);
            return currentTargetSpeed + (this.storedDiveEnergy * 1.2);
        } else {
            double penalizedSpeed = currentTargetSpeed - (baseThrust * 0.15 * climbMultiplier);
            return Math.max(penalizedSpeed, baseThrust * 0.7);
        }
    }

    private void handleLevelFlight() {
        this.storedDiveEnergy = Math.max(this.storedDiveEnergy - 0.005, 0.0);
    }

    private Vec3 calculateFlightMotion(float pitch, Vec3 lookAngle, double baseThrust) {
        if (this.currentForwardSpeed < 0.05 && pitch < -15.0F && this.storedDiveEnergy < 0.2) {
            this.stallTicks++;
        } else {
            this.stallTicks = Math.max(this.stallTicks - 2, 0);
        }

        if (this.stallTicks > 10) {
            Vec3 stallFall = this.getDeltaMovement().add(0, -0.15, 0).scale(0.9);
            this.currentForwardSpeed *= 0.8;
            this.storedDiveEnergy     = 0.0;
            return stallFall;
        } else {
            Vec3 controlledMotion = lookAngle.scale(this.currentForwardSpeed);

            if (this.currentForwardSpeed < baseThrust && pitch <= 5.0F && !this.onGround()) {
                controlledMotion = controlledMotion.add(0, -0.01, 0);
            }

            if (pitch < -5.0F && this.currentForwardSpeed > 0.1) {
                controlledMotion = controlledMotion.add(0, 0.03, 0);
            }

            return controlledMotion;
        }
    }

    private void spawnBubbles() {
        double speed = this.getDeltaMovement().length();
        double bubbleProbability = Mth.clamp(speed * 2.0, 0.15, 1.0);
        if (this.random.nextFloat() < bubbleProbability) {
            float yRot    = this.getYRot();
            float xRot    = Mth.clamp(this.getXRot(), -10.0F, 10.0F);
            Vec3  mouthDir = this.calculateViewVector(xRot, yRot);
            double spread = this.random.nextDouble() * 0.8 * (1.0 + speed);
            double dx = (this.random.nextFloat() - 0.5) * spread;
            double dy = (this.random.nextFloat() - 0.5) * spread;
            double dz = (this.random.nextFloat() - 0.5) * spread;
            this.level().addParticle(ParticleTypes.BUBBLE,
                    this.getX() - mouthDir.x * 1.1,
                    this.getY() - mouthDir.y + 0.25,
                    this.getZ() - mouthDir.z * 1.1,
                    dx, dy, dz);
        }
    }

    @Override
    protected void executeRidersJump(float amount, @NotNull Player controller) {
        if (this.dashTicksRemaining > DASH_DURATION_TICKS - 10) {
            return;
        }

        this.dashCombo = Math.min(this.dashCombo + 1, MAX_DASH_COMBO);
        this.idleTicks = 0;

        float effectiveAmount = Math.max(amount, 0.5F);
        double comboMultiplier = 1.0 + (this.dashCombo * COMBO_BONUS_PER_STACK);

        double dashBoost = ((this.isInWater() ? 1.2F : 0.5F) * effectiveAmount)
                * (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * comboMultiplier)
                * this.getBlockSpeedFactor();

        this.stallTicks = 0;
        Vec3 lookDir = controller.getLookAngle();
        double baseDashSpeed = dashBoost * 4.5;
        double targetSpeed = this.currentForwardSpeed + baseDashSpeed;

        double maxAllowedSpeed = 5.0 + (this.dashCombo * 0.8);
        double finalDashSpeed = Math.min(targetSpeed, maxAllowedSpeed);

        this.dashVector = lookDir.scale(finalDashSpeed);
        this.dashTicksRemaining = DASH_DURATION_TICKS;
        this.smoothedLookAngle = lookDir;

        this.currentForwardSpeed = finalDashSpeed;
        this.storedDiveEnergy = Math.min(this.storedDiveEnergy + (dashBoost * 1.5), 2.5);

        try {
            if (dashCooldownField != null) {
                dashCooldownField.setInt(this, 40);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        animationManager.play(NautilusAnimationManager.Animation.BELLY);

        this.setDashing(true);
        this.needsSync = true;
    }

    public @NotNull org.bukkit.entity.Nautilus getBukkitNautilus() {
        return (org.bukkit.entity.Nautilus) getBukkitEntity();
    }
}