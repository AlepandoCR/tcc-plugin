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

class ConfigCommand(
    private val plugin: TCCPlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(PermissionNode.CONFIG_USE)) {
            sender.sendComponent(Component.text("You don't have permission to use config commands.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /config reload").color(NamedTextColor.YELLOW))
            return true
        }

        return when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission(PermissionNode.CONFIG_RELOAD)) {
                    sender.sendComponent(Component.text("You don't have permission to reload configs.").color(NamedTextColor.RED))
                    true
                } else {
                    val count = plugin.configManager.reloadAll()

                    plugin.dataDrivenManager.reload().thenAccept {
                        plugin.logger.fine("datadriven files loaded for tcc plugin")

                        plugin.server
                            .scheduler
                            .runTask(plugin, Runnable {
                                plugin.raidManager.reload()
                                sender.sendComponent(
                                    Component.text("Reloaded $count config(s) and synchronized Raids.").color(NamedTextColor.GREEN)
                                )
                            })
                    }.exceptionally { ex ->
                        plugin.logger.severe("Error reloading data-driven configs: ${ex.message}")
                        null
                    }
                    true
                }
            }

            else -> {
                sender.sendComponent(Component.text("Unknown subcommand '${args[0]}'. Use reload.").color(NamedTextColor.RED))
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        return when (args.size) {
            1 -> listOf("reload").filter { it.startsWith(args[0].lowercase()) }
            else -> emptyList()
        }
    }
}

