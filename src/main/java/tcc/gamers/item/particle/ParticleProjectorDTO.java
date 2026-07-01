package tcc.gamers.item.particle;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class ParticleProjectorDTO implements Serializable {

    private final double radius;
    private final int amount;
    @NotNull private final String particleName;

    private transient Particle cachedParticle;

    public ParticleProjectorDTO(
            double radius,
            int amount,
            @NotNull String particleName
    ) {
        this.radius = radius;
        this.amount = amount;
        this.particleName = particleName;
        this.cachedParticle = parseParticle(particleName);
    }

    public ParticleProjectorDTO(@NotNull ItemStack item) {
        this.radius = ParticleProjectorHelper.getRadius(item);
        this.amount = ParticleProjectorHelper.getAmount(item);
        this.particleName = ParticleProjectorHelper.getParticleName(item);
        this.cachedParticle = parseParticle(this.particleName);
    }

    public double getRadius() {
        return radius;
    }

    public int getAmount() {
        return amount;
    }

    @NotNull
    public String getParticleName() {
        return particleName;
    }


    @NotNull
    public Particle getParticle() {
        if (cachedParticle == null) {
            cachedParticle = parseParticle(this.particleName);
        }
        return cachedParticle;
    }

    public void applyToItem(@NotNull ItemStack item) {
        ParticleProjectorHelper.updateSettings(item, this.radius, this.amount, this.particleName);
    }


    private @NotNull Particle parseParticle(@NotNull String name) {
        String cleaned = name.toLowerCase().trim().replace(" ", "_");
        if (cleaned.startsWith("minecraft:")) {
            cleaned = cleaned.substring("minecraft:".length());
        }

        try {
            var registryAccess = RegistryAccess.registryAccess();
            Particle p = registryAccess.getRegistry(RegistryKey.PARTICLE_TYPE)
                    .get(new NamespacedKey("minecraft", cleaned));
            if (p != null) return p;
        } catch (Exception ignored) {}

        try {
            Particle p = org.bukkit.Registry.PARTICLE_TYPE.get(new NamespacedKey("minecraft", cleaned));
            if (p != null) return p;
        } catch (Exception ignored) {}

        try {
            return Particle.valueOf(name.toUpperCase().trim().replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {}

        return Particle.FLAME;
    }
}