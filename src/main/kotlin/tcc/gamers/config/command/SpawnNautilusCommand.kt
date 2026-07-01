package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.item.dragon.DragonHornHelper
import tcc.gamers.nms.nautilus.DragonMountNautilus
import tcc.gamers.util.sendComponent

class SpawnNautilusCommand(private val plugin: TCCPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.isOp) {
            sender.sendComponent(Component.text("No tienes permiso o no eres un jugador.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) return false

        when (args[0].lowercase()) {
            "spawn" -> {
                try {
                    val customNautilus = DragonMountNautilus(sender.world, plugin, sender)
                    customNautilus.spawn(sender.location)
                    sender.sendComponent(Component.text("Nautilus invocado.").color(NamedTextColor.GREEN))
                } catch (e: Exception) {
                    sender.sendComponent(Component.text("Error: ${e.message}").color(NamedTextColor.RED))
                }
            }
            "horn" -> {
                val souls = args.getOrNull(1)?.toIntOrNull() ?: 0
                sender.inventory.addItem(DragonHornHelper.createDragonHorn(souls))
                sender.sendComponent(Component.text("Has recibido un Cuerno de Dragón con $souls almas.").color(NamedTextColor.GOLD))
            }
            "soul" -> {
                val amount = args.getOrNull(1)?.toIntOrNull() ?: 1
                sender.inventory.addItem(DragonHornHelper.createSoulShard(amount))
                sender.sendComponent(Component.text("Has recibido $amount Fragmentos de Alma.").color(NamedTextColor.AQUA))
            }
            "race" -> {
                val arg = args.getOrNull(1)
                if (arg == "reload") {
                    plugin.racePathManager
                    sender.sendComponent(Component.text("Configuración recargada.").color(NamedTextColor.GREEN))
                }
            }
            else -> sender.sendComponent(Component.text("Uso: /spawnnautilus <spawn|horn|soul>").color(NamedTextColor.RED))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("spawn", "horn", "soul", "race").filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2) {
            return when (args[0].lowercase()) {
                "horn" -> listOf("<cantidad_almas>")
                "soul" -> listOf("<cantidad>")
                "race" -> listOf("reload")
                else -> emptyList()
            }
        }
        return emptyList()
    }
}