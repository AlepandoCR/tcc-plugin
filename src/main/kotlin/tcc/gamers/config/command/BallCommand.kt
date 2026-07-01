package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.checkerframework.checker.units.qual.C
import tcc.gamers.TCCPlugin
import tcc.gamers.ball.Ball
import tcc.gamers.item.ball.SoccerBallHelper
import tcc.gamers.item.ball.SoccerCleatsHelper
import tcc.gamers.item.dragon.DragonHornHelper
import tcc.gamers.nms.nautilus.DragonMountNautilus
import tcc.gamers.util.sendComponent

class BallCommand(private val plugin: TCCPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.isOp) {
            sender.sendComponent(Component.text("No tienes permiso o no eres un jugador.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "spawn" -> {
                try {
                    val ball = Ball(sender.world, plugin)
                    ball.spawn(sender.location)
                    sender.sendComponent(Component.text("Bola invocada.").color(NamedTextColor.GREEN))
                } catch (e: Exception) {
                    sender.sendComponent(Component.text("Error: ${e.message}").color(NamedTextColor.RED))
                }
            }
            "item" -> {
                sender.inventory.addItem(SoccerBallHelper.createSoccerBall())
                sender.sendComponent(
                    Component.text("Has una pelota de futbol").color(NamedTextColor.GOLD)
                )
            }
            "botas" -> {
                sender.inventory.addItem(SoccerCleatsHelper.createSoccerCleats())
                sender.sendComponent(
                    Component.text("Has recibido unas botas de futbol").color(NamedTextColor.GOLD)
                )
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("spawn", "item", "botas").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}