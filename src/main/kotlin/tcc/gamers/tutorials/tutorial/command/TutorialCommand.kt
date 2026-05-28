package tcc.gamers.tutorials.tutorial.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.hasPermission
import tcc.gamers.util.runTutorial
import tcc.gamers.util.sendComponent
import java.util.UUID

class TutorialCommand(private val plugin: TCCPlugin) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(PermissionNode.TUTORIALS_START)) {
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /tutorials <tutorialName>").color(NamedTextColor.YELLOW))
            return true
        }

        val tutorialName = args[0]

        if (sender !is Player) {
            if (sender is ConsoleCommandSender) {
                if (args.size < 2) {
                    sender.sendComponent(Component.text("No player identifier specified!").color(NamedTextColor.RED))
                    return false
                }

                runTutorialFromConsole(sender, tutorialName, args[1])
            }
            return true
        }

        sender.runTutorial(tutorialName, plugin)
        return true
    }

    private fun runTutorialFromConsole(
        sender: ConsoleCommandSender,
        tutorialName: String,
        playerIdentifier: String
    ) {
        val player = plugin.server.getPlayer(playerIdentifier)
        if (player != null) {
            player.runTutorial(tutorialName, plugin)
            sender.sendComponent(Component.text("Tutorial '$tutorialName' started for player ${player.name}.").color(NamedTextColor.GREEN))
            return
        }

        try {
            val uuid = UUID.fromString(playerIdentifier)
            val playerFromUUID = plugin.server.getPlayer(uuid)

            if (playerFromUUID != null) {
                playerFromUUID.runTutorial(tutorialName, plugin)
                sender.sendComponent(Component.text("Tutorial '$tutorialName' started for player ${playerFromUUID.name}").color(NamedTextColor.GREEN))
            } else {
                sender.sendComponent(Component.text("No online player found with UUID: $playerIdentifier").color(NamedTextColor.RED))
            }
        } catch (_: IllegalArgumentException) {
            sender.sendComponent(Component.text("Invalid UUID").color(NamedTextColor.RED))
        }
    }
}
