package tcc.gamers.permadeath;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tcc.gamers.TCCPlugin;
import tcc.gamers.data.GeneratedPlayerDeathsDto;
import tcc.gamers.data.PlayerDeathsDto;
import tcc.gamers.util.DataDrivenLoader;
import tcc.gamers.util.StorageFolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PermaDeathManager {
    private final @NotNull Map<String, PlayerDeathsDto> playerDeathsMap;
    private final @NotNull TCCPlugin plugin;
    private final @NotNull DataDrivenLoader dataDrivenLoader;
    private final @NotNull StorageFolder storage;

    private boolean enabled = true;

    public PermaDeathManager(@NotNull TCCPlugin plugin) {
        this.playerDeathsMap = new ConcurrentHashMap<>();
        this.plugin = plugin;
        this.dataDrivenLoader = new DataDrivenLoader(plugin);
        this.storage = StorageFolder.PERMA_DEATH_DATA;
    }

    public void disable(){
        enabled = false;
    }

    public void enable(){
        enabled = true;
    }

    public CompletableFuture<Void> loadAllAsync() {
        dataDrivenLoader.loadDirectory(
                storage,
                GeneratedPlayerDeathsDto.class,
                PlayerDeathsDto::new,
                (List<PlayerDeathsDto> loaded) -> {
                    playerDeathsMap.clear();
                    for (PlayerDeathsDto dto : loaded) {
                        playerDeathsMap.put(dto.getPlayerUniqueIdentifier(), dto);
                    }
                },
                "Deaths"
        );

        return dataDrivenLoader.loadAll();
    }

    public CompletableFuture<Void> saveAsync(@NotNull PlayerDeathsDto dto) {
        return dataDrivenLoader.saveToDirectory(
                storage,
                dto.getPlayerUniqueIdentifier(),
                dto,
                GeneratedPlayerDeathsDto.class,
                data -> Map.of(
                        "playerUniqueIdentifier", data.getPlayerUniqueIdentifier(),
                        "deaths", data.getDeaths(),
                        "lives", data.getLives()
                )
        ).thenAccept(_ -> plugin.getLogger().fine("Saved perma death data for: " + dto.getPlayerUniqueIdentifier()));
    }

    public CompletableFuture<Void> saveAllAsync() {
        CompletableFuture<?>[] futures = playerDeathsMap.values().stream()
                .map(this::saveAsync)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenRun(() -> plugin.getLogger().info("All PermaDeath data saved successfully."));
    }

    public void addPlayer(@NotNull Player player) {
        String uuid = player.getUniqueId().toString();

        PlayerDeathsDto dto = playerDeathsMap.computeIfAbsent(
                uuid,
                PlayerDeathsDto::new
        );
        saveAsync(dto);
    }

    public @NotNull Collection<PlayerDeathsDto> getPlayerDeaths() {
        return playerDeathsMap.values();
    }

    public void onPlayerDeath(@NotNull Player player) {
        if(!enabled) return;

        getDeathsDtoFor(player).ifPresent(dto -> {
            dto.addDeath();

            checkPermaDeath(player, dto);

            saveAsync(dto);
        });
    }

    public void checkPermaDeath(@NotNull Player player, @NotNull PlayerDeathsDto dto) {
        if (dto.isPermaDeath()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                var faceComponent = getPlayerFaceComponent(player.getUniqueId()).orElse(Component.text(""));

                Component deathMessage = Component.text()
                        .append(Component.text("\n"))
                        .append(Component.text(" ☠ ", NamedTextColor.DARK_RED))
                        .append(Component.text(player.getName(), NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(" "))
                        .append(faceComponent)
                        .append(Component.text(" ha caído definitivamente.", NamedTextColor.GRAY))
                        .append(Component.text("\n"))
                        .append(Component.text("    ▪ Razón: ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Límite de muertes alcanzado (PermaDeath)", NamedTextColor.RED))
                        .append(Component.text("\n"))
                        .build();

                plugin.getServer().broadcast(deathMessage);

                plugin.getServer()
                        .getOnlinePlayers()
                        .forEach(onlinePlayer ->
                                onlinePlayer.playSound(
                                        Sound.sound(
                                                (builder -> {
                                                    builder.type(org.bukkit.Sound.ENTITY_WITHER_SPAWN);
                                                    builder.source(Sound.Source.MASTER);
                                                    builder.volume(1.0f);
                                                    builder.pitch(1.0f);
                                                })
                                        )
                                )
                        );

                player.ban(
                        "PermaDeath: Llegaste a tu límite de muertes",
                        (Date) null, // forever
                        null,
                        false // kick = false
                );

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.kick(
                                deathMessage.append(Component.newline()
                                        .append(Component.text(" Gracias por participar en ").color(NamedTextColor.GRAY))
                                        .append(Component.text("Reinos en Ruinas").color(NamedTextColor.LIGHT_PURPLE))
                                )
                        );
                    }
                }, 20L * 5);
            });
        }
    }

    private @NotNull Optional<Component> getPlayerFaceComponent(@NotNull UUID uuid) {
        var player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }

        var headObject = ObjectContents
                .playerHead()
                .id(uuid)
                .hat(true)
                .build();

        return Optional.of(Component.object(headObject));
    }

    public void onPlayerRestoreLive(@NotNull Player player) {
        getDeathsDtoFor(player).ifPresent(dto -> {
            dto.addLive();
            saveAsync(dto);
        });
    }

    private @NotNull Optional<PlayerDeathsDto> getDeathsDtoFor(@NotNull Player player) {
        return Optional.ofNullable(playerDeathsMap.get(player.getUniqueId().toString()));
    }
}