package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import tcc.gamers.item.particle.ParticleProjectorHelper

class ParticleProjectorCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(
                Component.text(
                    "Este comando solo puede ser ejecutado por un jugador.",
                    NamedTextColor.RED
                )
            )
            return true
        }

        if (!sender.isOp) {
            sender.sendMessage(Component.text("No tienes permisos para usar este comando.", NamedTextColor.RED))
            return true
        }

        val projector = ParticleProjectorHelper.createParticleProjector(1.5, 10, "FLAME")

        sender.inventory.addItem(projector)

        sender.sendMessage(
            Component.text("¡Has recibido un ", NamedTextColor.GREEN)
                .append(Component.text("Proyector de Partículas", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" exitosamente!", NamedTextColor.GREEN))
        )

        return true
    }
}