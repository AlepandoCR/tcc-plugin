package tcc.gamers.ai.event.skript;

import org.bukkit.entity.Player;
import org.skriptlang.skript.addon.SkriptAddon;
import tcc.gamers.ai.event.RaidCompleteEvent;
import tcc.gamers.ai.event.RaidPlayerJoinEvent;
import tcc.gamers.ai.event.RaidPlayerLeaveEvent;

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

    }
}