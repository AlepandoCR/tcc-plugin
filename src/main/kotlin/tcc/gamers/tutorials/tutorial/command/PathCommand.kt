package tcc.gamers.tutorials.tutorial.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.manager.PathManager
import tcc.gamers.tutorials.tutorial.session.SessionManager
import tcc.gamers.tutorials.tutorial.session.PathCaptureSession
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.hasPermission
import tcc.gamers.util.sendComponent

class PathCommand(
    private val plugin: TCCPlugin,
    private val pathManager: PathManager,
    private val sessionManager: SessionManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission(PermissionNode.PATH_USE)) {
            sender.sendComponent(Component.text("You don't have permission to use path commands.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /path <create|edit|delete|list|cancel> [...]").color(NamedTextColor.YELLOW))
            return true
        }
        when (args[0].lowercase()) {
            "create" -> {
                if (sender !is Player) { sender.sendComponent(Component.text("Only players can create paths.").color(NamedTextColor.RED)); return true }
                if (!sender.hasPermission("path.create")) { sender.sendComponent(Component.text("You don't have permission to create paths.").color(NamedTextColor.RED)); return true }
                if (args.size < 2) { sender.sendComponent(Component.text("Usage: /path create <id>").color(NamedTextColor.YELLOW)); return true }
                val id = args[1]
                if (pathManager.pathExists(id)) { sender.sendComponent(Component.text("Path $id already exists.").color(NamedTextColor.RED)); return true }
                val session = PathCaptureSession(UUID.randomUUID(), sender, id, pathManager, plugin, false)
                val started = sessionManager.startSession(session)
                if (!started) sender.sendComponent(Component.text("You already have an active session.").color(NamedTextColor.RED)) else sender.sendComponent(Component.text("Path creation session started: $id").color(NamedTextColor.GREEN))
                return true
            }
            "edit" -> {
                if (sender !is Player) { sender.sendComponent(Component.text("Only players can edit paths.").color(NamedTextColor.RED)); return true }
                if (!sender.hasPermission("path.edit")) { sender.sendComponent(Component.text("You don't have permission to edit paths.").color(NamedTextColor.RED)); return true }
                if (args.size < 2) { sender.sendComponent(Component.text("Usage: /path edit <id>").color(NamedTextColor.YELLOW)); return true }
                val id = args[1]
                if (!pathManager.pathExists(id)) { sender.sendComponent(Component.text("Path $id does not exist.").color(NamedTextColor.RED)); return true }
                // check if already locked by another session: simple check via sessionManager
                val session = PathCaptureSession(UUID.randomUUID(), sender, id, pathManager, plugin, true)
                val started = sessionManager.startSession(session)
                if (!started) sender.sendComponent(Component.text("You already have an active session.").color(NamedTextColor.RED)) else sender.sendComponent(Component.text("Path edit session started: $id").color(NamedTextColor.GREEN))
                return true
            }
            "delete" -> {
                if (!sender.hasPermission(PermissionNode.PATH_DELETE)) { sender.sendComponent(Component.text("You don't have permission to delete paths.").color(NamedTextColor.RED)); return true }
                if (args.size < 2) { sender.sendComponent(Component.text("Usage: /path delete <id>").color(NamedTextColor.YELLOW)); return true }
                val id = args[1]
                if (!pathManager.pathExists(id)) { sender.sendComponent(Component.text("Path $id does not exist.").color(NamedTextColor.RED)); return true }
                val ok = pathManager.deletePath(id)
                if (ok) sender.sendComponent(Component.text("Path $id deleted.").color(NamedTextColor.GREEN)) else sender.sendComponent(Component.text("Failed to delete path $id.").color(NamedTextColor.RED))
                return true
            }
            "list" -> {
                if (!sender.hasPermission("path.list")) { sender.sendComponent(Component.text("You don't have permission to list paths.").color(NamedTextColor.RED)); return true }
                val ids = pathManager.listPathIds()
                sender.sendComponent(Component.text("Saved paths (${ids.size}): ${ids.joinToString(", ")}").color(NamedTextColor.AQUA))
                return true
            }
            "cancel", "stop" -> {
                if (sender !is Player) { sender.sendComponent(Component.text("Only players can cancel sessions.").color(NamedTextColor.RED)); return true }
                if (!sender.hasPermission("path.cancel")) { sender.sendComponent(Component.text("You don't have permission to cancel sessions.").color(NamedTextColor.RED)); return true }
                val session = sessionManager.getSessionForPlayer(sender)
                if (session == null) { sender.sendComponent(Component.text("You have no active session.").color(NamedTextColor.YELLOW)); return true }
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
            return listOf("create","edit","delete","list","cancel").filter { it.startsWith(args[0].lowercase()) }
        }
        // Provide path id completions for subcommands that expect an existing id
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "edit", "delete" -> {
                    val partial = args[1].lowercase()
                    return pathManager.listPathIds().filter { it.lowercase().startsWith(partial) }
                }
                else -> return emptyList()
            }
        }

        return null
    }
}





