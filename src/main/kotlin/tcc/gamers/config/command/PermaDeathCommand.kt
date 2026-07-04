package tcc.permadeath.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import tcc.gamers.TCCPlugin
import tcc.gamers.util.sendComponent

class PermaDeathCommand(private val plugin: TCCPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendComponent(Component.text("No tienes permiso para usar este comando.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Uso: /permadeath <enable|disable|addlive|removelive|adddeath|removedeath> [jugador]").color(NamedTextColor.RED))
            return true
        }

        val manager = plugin.permaDeathManager
        when (val action = args[0].lowercase()) {
            "enable" -> {
                manager.enable()
                sender.sendComponent(Component.text("El sistema de PermaDeath ha sido activado.").color(NamedTextColor.GREEN))
            }

            "disable" -> {
                manager.disable()
                sender.sendComponent(Component.text("El sistema de PermaDeath ha sido desactivado.").color(NamedTextColor.RED))
            }

            "addlive", "removelive", "adddeath", "removedeath" -> {
                if (args.size < 2) {
                    sender.sendComponent(Component.text("Debes especificar el nombre de un jugador en línea.").color(NamedTextColor.RED))
                    return true
                }

                val targetName = args[1]
                val targetPlayer = Bukkit.getPlayerExact(targetName)

                // Verify the player is currently online
                if (targetPlayer == null || !targetPlayer.isOnline) {
                    sender.sendComponent(Component.text("El jugador '$targetName' no está en línea.").color(NamedTextColor.RED))
                    return true
                }

                val uuidString = targetPlayer.uniqueId.toString()

                // Fetch the DTO from the collection
                val dto = manager.playerDeaths.firstOrNull { it.playerUniqueIdentifier == uuidString }

                if (dto == null) {
                    sender.sendComponent(Component.text("El jugador no tiene datos registrados aún.").color(NamedTextColor.RED))
                    return true
                }

                // Explicit matching for each action to ensure safety and predictability
                when (action) {
                    "addlive" -> {
                        dto.addLive()
                        manager.saveAsync(dto)
                        manager.checkPermaDeath(targetPlayer, dto)

                        sender.sendComponent(
                            Component.text("Se ha añadido una vida a ${targetPlayer.name}. Vidas actuales: ${dto.lives}")
                                .color(NamedTextColor.GREEN)
                        )
                    }
                    "removelive" -> {
                        dto.removeLive()
                        manager.saveAsync(dto)
                        manager.checkPermaDeath(targetPlayer, dto)

                        sender.sendComponent(
                            Component.text("Se ha quitado una vida a ${targetPlayer.name}. Vidas actuales: ${dto.lives}")
                                .color(NamedTextColor.YELLOW)
                        )
                    }
                    "adddeath" -> {
                        dto.addDeath()
                        manager.saveAsync(dto)
                        manager.checkPermaDeath(targetPlayer, dto)

                        sender.sendComponent(
                            Component.text("Se ha añadido una muerte a ${targetPlayer.name}. Muertes actuales: ${dto.deaths}")
                                .color(NamedTextColor.RED)
                        )
                    }
                    "removedeath" -> {
                        dto.removeDeath()
                        manager.saveAsync(dto)
                        manager.checkPermaDeath(targetPlayer, dto)

                        sender.sendComponent(
                            Component.text("Se ha restado una muerte a ${targetPlayer.name}. Muertes actuales: ${dto.deaths}")
                                .color(NamedTextColor.GREEN)
                        )
                    }
                }
            }
            else -> {
                sender.sendComponent(Component.text("Subcomando desconocido.").color(NamedTextColor.RED))
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        // Autocomplete for the first argument
        if (args.size == 1) {
            return listOf("enable", "disable", "addlive", "removelive", "adddeath", "removedeath")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        // Autocomplete for the second argument (online player names)
        if (args.size == 2) {
            val action = args[0].lowercase()
            if (action in listOf("addlive", "removelive", "adddeath", "removedeath")) {
                return Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            }
        }

        return emptyList()
    }
}