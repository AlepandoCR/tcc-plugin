package tcc.gamers.tutorials.tutorial.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.data.DragonRaceConfig
import tcc.gamers.tutorials.tutorial.manager.RacePathManager
import tcc.gamers.tutorials.tutorial.session.RacePathCaptureSession
import tcc.gamers.tutorials.tutorial.session.SessionManager
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.StorageFolder
import tcc.gamers.util.hasPermission
import tcc.gamers.util.sendComponent
import java.io.File
import java.util.*

class RacePathCommand(
    private val plugin: TCCPlugin,
    private val racePathManager: RacePathManager,
    private val sessionManager: SessionManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission(PermissionNode.PATH_USE)) {
            sender.sendComponent(Component.text("You don't have permission to use racepath commands.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /racepath <create|edit|delete|list|cancel|reload> [id]").color(NamedTextColor.YELLOW))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                sender.sendComponent(Component.text("Reloading race paths and clearing memory...").color(NamedTextColor.GRAY))

                racePathManager.reloadAsync().thenAccept {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        sender.sendComponent(Component.text("All race paths reloaded cleanly from disk!").color(NamedTextColor.GREEN))
                    })
                }

                plugin.raceManager.reload()
                return true
            }
            "create" -> {
                if (sender !is Player) { sender.sendMessage("Only players can execute this subcommand."); return true }

                if (args.size < 8) {
                    sender.sendComponent(Component.text("Usage: /racepath create <id> <xRadius> <yRadius> <zRadius> <tickFrequency> <checkpointRadius> <displayName...>").color(NamedTextColor.RED))
                    return true
                }

                val id = args[1]
                if (racePathManager.pathExists(id)) {
                    sender.sendComponent(Component.text("A race path with ID '$id' already exists.").color(NamedTextColor.RED))
                    return true
                }

                try {
                    val xRadius = args[2].toDouble()
                    val yRadius = args[3].toDouble()
                    val zRadius = args[4].toDouble()
                    val tickFrequency = args[5].toDouble()
                    val checkpointDetectionRadius = args[6].toDouble()
                    val displayName = args.drop(7).joinToString(" ")

                    val loc = sender.location

                    val dragonConfig = DragonRaceConfig(
                        loc.x, loc.y, loc.z, xRadius, yRadius, zRadius, tickFrequency,
                        checkpointDetectionRadius, loc.world.name, id, displayName
                    )

                    val folder = StorageFolder.DRAGON_RACES.getFolder(plugin)
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }

                    val configFile = File(folder, "$id.yml")

                    try {
                        dragonConfig.save(configFile)

                        if (configFile.exists()) {
                            sender.sendComponent(Component.text("Race config saved as $id.yml").color(NamedTextColor.GREEN))
                        } else {
                            sender.sendComponent(Component.text("Failed) to save race config").color(NamedTextColor.RED))
                        }
                    } catch (_: Exception) {
                        sender.sendComponent(Component.text("Failed) to save race config").color(NamedTextColor.RED))
                    }

                    val session = RacePathCaptureSession(UUID.randomUUID(), sender, id, racePathManager, plugin, editExisting = false)
                    val started = sessionManager.startSession(session)

                    if (!started) {
                        sender.sendComponent(Component.text("You already have an active session.").color(NamedTextColor.RED))
                    } else {
                        sender.sendComponent(Component.text("Started creating race path '$id'. Right click to add points.").color(NamedTextColor.GREEN))
                    }
                } catch (_: NumberFormatException) {
                    sender.sendComponent(Component.text("Error, invalid number .").color(NamedTextColor.RED))
                }

                return true
            }
            "edit" -> {
                if (sender !is Player) { sender.sendMessage("Only players can execute this subcommand."); return true }
                if (args.size < 2) { sender.sendComponent(Component.text("Usage: /racepath edit <id>").color(NamedTextColor.RED)); return true }
                val id = args[1]

                if (!racePathManager.pathExists(id) && racePathManager.getLoadedPath(id) == null) {
                    sender.sendComponent(Component.text("Race path '$id' does not exist.").color(NamedTextColor.RED))
                    return true
                }

                racePathManager.loadPathAsync(id).thenAccept { racePath ->
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (racePath == null) {
                            sender.sendComponent(Component.text("Race path '$id' not found.").color(NamedTextColor.RED))
                            return@Runnable
                        }

                        val session = RacePathCaptureSession(UUID.randomUUID(), sender, id, racePathManager, plugin, editExisting = true)
                        val started = sessionManager.startSession(session)

                        if (!started) {
                            sender.sendComponent(Component.text("You already have an active session.").color(NamedTextColor.RED))
                        } else {
                            sender.sendComponent(Component.text("Editing race path '$id'. Current locations loaded.").color(NamedTextColor.GREEN))
                        }
                    })
                }
                plugin.raceManager.reload()

                return true
            }
            "delete" -> {
                if (args.size < 2) { sender.sendComponent(Component.text("Usage: /racepath delete <id>").color(NamedTextColor.RED)); return true }
                val id = args[1]

                racePathManager.deletePathAsync(id).thenAccept { success ->
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (success) {
                            sender.sendComponent(Component.text("Race path '$id' deleted successfully.").color(NamedTextColor.GREEN))
                        } else {
                            sender.sendComponent(Component.text("Could not delete race path '$id'. Does it exist?").color(NamedTextColor.RED))
                        }
                    })
                }
                return true
            }
            "list" -> {
                val ids = racePathManager.listPathIds()
                if (ids.isEmpty()) {
                    sender.sendComponent(Component.text("No race paths found.").color(NamedTextColor.YELLOW))
                } else {
                    sender.sendComponent(Component.text("Available race paths: ${ids.joinToString(", ")}").color(NamedTextColor.AQUA))
                }
                return true
            }
            "cancel" -> {
                if (sender !is Player) { sender.sendMessage("Only players can execute this subcommand."); return true }
                val session = sessionManager.getSessionForPlayer(sender)

                if (session == null) {
                    sender.sendComponent(Component.text("You have no active session.").color(NamedTextColor.YELLOW))
                    return true
                }

                sessionManager.stopSessionForPlayer(sender, save = false)
                sender.sendComponent(Component.text("Session canceled.").color(NamedTextColor.GREEN))
                return true
            }
            else -> {
                sender.sendComponent(Component.text("Unknown subcommand ${args[0]}").color(NamedTextColor.RED))
                return true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            return listOf("create", "edit", "delete", "list", "cancel", "reload").filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size >= 2 && args[0].lowercase() == "create") {
            return when (args.size) {
                2 -> listOf("<id>")
                3 -> listOf("<xRadius>")
                4 -> listOf("<yRadius>")
                5 -> listOf("<zRadius>")
                6 -> listOf("<tickFrequency>")
                7 -> listOf("<checkpointDetectionRadius>")
                8 -> listOf("<displayName...>")
                else -> emptyList()
            }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "edit", "delete" -> {
                    val partial = args[1].lowercase()
                    return racePathManager.listPathIds().filter { it.lowercase().startsWith(partial) }
                }
                else -> return emptyList()
            }
        }
        return null
    }
}