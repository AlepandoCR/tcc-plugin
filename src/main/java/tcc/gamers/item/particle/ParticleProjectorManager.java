package tcc.gamers.item.particle;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleProjectorManager {

    private final Map<UUID, ParticleProjectorDTO> activeProjectors = new ConcurrentHashMap<>();

    private final @NotNull TCCPlugin plugin;

    public ParticleProjectorManager(@NotNull TCCPlugin plugin) {
        this.plugin = plugin;
    }

    public void startLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeProjectors.keySet()) {
                    Player player = plugin.getServer().getPlayer(uuid);

                    if (player == null || !player.isOnline()) continue;

                    ParticleProjectorDTO dto = activeProjectors.get(uuid);

                    Particle particle = dto.getParticle();
                    double radius = dto.getRadius();
                    int amount = dto.getAmount();

                    player.getWorld().spawnParticle(
                            particle,
                            player.getLocation(),
                            amount,
                            radius,
                            radius,
                            radius,
                            0
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public void checkPlayerEquipped(@NotNull Player player) {
        ItemStack particleProjector =
                Arrays.stream(
                                player
                                        .getInventory()
                                        .getContents()
                        ).filter(Objects::nonNull)
                        .filter(ParticleProjectorHelper::isParticleProjector)
                        .findAny()
                        .orElse(null);

        if (particleProjector != null) {
            activeProjectors.put(player.getUniqueId(), new ParticleProjectorDTO(particleProjector));
        } else {
            activeProjectors.remove(player.getUniqueId());
        }
    }

    public void removePlayer(@NotNull Player player) {
        activeProjectors.remove(player.getUniqueId());
    }
}