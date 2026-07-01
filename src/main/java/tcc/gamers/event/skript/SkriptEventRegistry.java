package tcc.gamers.event.skript;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.skriptlang.skript.addon.SkriptAddon;
import tcc.gamers.event.race.EntityCrossCheckpointEvent;
import tcc.gamers.event.RaidCompleteEvent;
import tcc.gamers.event.RaidPlayerJoinEvent;
import tcc.gamers.event.RaidPlayerLeaveEvent;

import java.util.UUID;

public class SkriptEventRegistry {

    public static void registerAll(SkriptAddon addon) {

        new BaseSkriptEvent<>(addon, "Raid Join", RaidPlayerJoinEvent.class, "raid join") {
            @Override
            protected void registerEventValues() {
                registerValue(Player.class, RaidPlayerJoinEvent::getPlayer);
                registerValue(String.class, e -> e.getRaid().getIdentifier());
            }
        }.register();

        new BaseSkriptEvent<>(addon, "Raid Leave", RaidPlayerLeaveEvent.class, "raid leave") {
            @Override
            protected void registerEventValues() {
                registerValue(Player.class, RaidPlayerLeaveEvent::getPlayer);
                registerValue(String.class, e -> e.getRaid().getIdentifier());
            }
        }.register();

        new BaseSkriptEvent<>(addon, "Raid Complete", RaidCompleteEvent.class, "raid complete") {
            @Override
            protected void registerEventValues() {
                registerValue(Player.class, RaidCompleteEvent::getPlayer);
                registerValue(String.class, e -> e.getRaid().getIdentifier());
                registerValue(Integer.class, RaidCompleteEvent::getKarma);
            }
        }.register();

        new BaseSkriptEvent<>(addon, "Race checkpoint", EntityCrossCheckpointEvent.class, "race checkpoint") {
            @Override
            protected void registerEventValues() {
                registerValue(UUID.class, EntityCrossCheckpointEvent::getEntityUuid);
                registerValue(String.class, e -> e.getRace().getIdentifier());
                registerValue(Integer.class, EntityCrossCheckpointEvent::getCheckpointIndex);
                registerValue(Boolean.class, EntityCrossCheckpointEvent::isGoal);
                registerValue(Location.class, EntityCrossCheckpointEvent::getCheckpointLocation);
            }
        }.register();

    }
}