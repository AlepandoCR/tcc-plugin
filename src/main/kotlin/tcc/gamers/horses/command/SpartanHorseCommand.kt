package tcc.gamers.horses.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.nms.horses.TaxiSpawner
import tcc.gamers.tutorials.tutorial.manager.PathManager
import tcc.gamers.util.path.Path
import tcc.gamers.util.path.PathMode
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.hasPermission
import tcc.gamers.util.sendComponent
import tcc.gamers.ui.taxi.TaxiMenu


class SpartanHorseCommand(
    private val plugin: TCCPlugin,
    private val pathManager: PathManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(PermissionNode.TAXI_USE)) {
            sender.sendComponent(Component.text("You don't have permission to use taxi commands.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /taxi <spawn|menu>").color(NamedTextColor.YELLOW))
            return true
        }

        when (args[0].lowercase()) {
            "spawn" -> handleSpawn(sender, args)
            "menu" -> {
                if (sender !is Player) {
                    sender.sendComponent(Component.text("Only players can open the taxi menu.").color(NamedTextColor.RED))
                    return true
                }
                TaxiMenu.open(plugin, sender)
                return true
            }
            else -> {
                sender.sendComponent(Component.text("Unknown subcommand '${args[0]}'. Use spawn or menu.").color(NamedTextColor.RED))
                return true
            }
        }
        return false
    }

    private fun handleSpawn(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendComponent(Component.text("Only players can spawn a taxi.").color(NamedTextColor.RED))
            return true
        }
        if (!sender.hasPermission(PermissionNode.TAXI_SPAWN)) {
            sender.sendComponent(Component.text("You don't have permission to spawn taxi horses.").color(NamedTextColor.RED))
            return true
        }
        if (args.size < 3) {
            sender.sendComponent(Component.text("Usage: /taxi spawn <pathId> <start_finish|finish_start>").color(NamedTextColor.YELLOW))
            return true
        }

        val pathId = args[1]
        val directionToken = args[2].lowercase()
        val pathMode = when (directionToken) {
            "start_finish" -> PathMode.CLOSEST_TO_FURTHEST
            "finish_start" -> PathMode.FURTHEST_TO_CLOSEST
            else -> {
                sender.sendComponent(Component.text("Invalid direction. Use start_finish or finish_start.").color(NamedTextColor.RED))
                return true
            }
        }

        val loadedPath = pathManager.loadPath(pathId)
        if (loadedPath == null) {
            sender.sendComponent(Component.text("Path '$pathId' does not exist.").color(NamedTextColor.RED))
            return true
        }

        val points = loadedPath.getLocationList()
        if (points.isEmpty()) {
            sender.sendComponent(Component.text("Path '$pathId' has no points.").color(NamedTextColor.RED))
            return true
        }

        val traversalPath = Path(sender.location, pathMode)
        points.forEach { traversalPath.addLocation(it.clone()) }

        TaxiSpawner.spawnTaxi(plugin, traversalPath, pathMode, sender.location.clone(), sender)
        sender.sendComponent(Component.text("Spawned taxi for path '$pathId' using direction '$directionToken'.").color(NamedTextColor.GREEN))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        return when (args.size) {
            1 -> listOf("spawn", "menu").filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0].equals("spawn", ignoreCase = true)) {
                pathManager.listPathIds().filter { it.startsWith(args[1], ignoreCase = true) }
            } else emptyList()
            3 -> if (args[0].equals("spawn", ignoreCase = true)) {
                listOf("start_finish", "finish_start").filter { it.startsWith(args[2].lowercase()) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
