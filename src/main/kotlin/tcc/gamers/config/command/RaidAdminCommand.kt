package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import tcc.gamers.TCCPlugin
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.hasPermission
import tcc.gamers.util.sendComponent
import java.util.UUID

class RaidAdminCommand(
    private val plugin: TCCPlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(PermissionNode.RAID_ADMIN)) {
            sender.sendComponent(Component.text("You don't have permission to manage raids.").color(NamedTextColor.RED))
            return true
        }

        if (args.size < 2) {
            sender.sendComponent(Component.text("Usage: /raidadmin <start|stop> <raid-id>").color(NamedTextColor.YELLOW))
            return true
        }

        val action = args[0].lowercase()
        val raidId = args[1]

        val raid = plugin.raidManager.raids.find { it.identifier.equals(raidId, ignoreCase = true) }

        if (raid == null) {
            sender.sendComponent(Component.text("Raid with ID '$raidId' not found.").color(NamedTextColor.RED))
            return true
        }

        when (action) {
            "start" -> {
                raid.start()
                sender.sendComponent(Component.text("Raid '$raidId' has been started.").color(NamedTextColor.GREEN))
            }
            "stop" -> {
                raid.stop()
                sender.sendComponent(Component.text("Raid '$raidId' has been stopped.").color(NamedTextColor.GREEN))
            }
            "trigger" -> {
                raid.spawnMobs()
                raid.startRaidTickLogic()
                sender.sendComponent(Component.text("Raid '$raidId' has been triggered.").color(NamedTextColor.GREEN))
            }
            else -> {
                sender.sendComponent(Component.text("Unknown action '$action'. Use start or stop.").color(NamedTextColor.RED))
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("start", "stop", "trigger").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> {
                val raidIds = plugin.raidManager.raids.map { it.identifier }
                raidIds.filter { it.startsWith(args[1], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}