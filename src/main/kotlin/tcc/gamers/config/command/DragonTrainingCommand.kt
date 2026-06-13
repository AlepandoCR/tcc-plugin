package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.ai.session.DragonTrainingSession
import tcc.gamers.ai.session.training.RandomTrainingPointGenerator
import tcc.gamers.util.sendComponent

class DragonTrainingCommand(private val plugin: TCCPlugin) : CommandExecutor, TabCompleter {


    /** The currently active training session, or null if none is running. */
    private var activeSession: DragonTrainingSession? = null


    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (sender !is Player || !sender.isOp) {
            sender.sendComponent(
                Component.text("No tienes permiso o no eres un jugador.").color(NamedTextColor.RED)
            )
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "start"  -> handleStart(sender, args)
            "stop"   -> handleStop(sender)
            "status" -> handleStatus(sender)
            else     -> sendUsage(sender)
        }

        return true
    }

    /**
     * `/dragontraining start <amount>`
     *
     * Parses dragon count, builds the point generator anchored at the sender's
     * current location, creates and starts the session.
     */
    private fun handleStart(sender: Player, args: Array<out String>) {
        if (activeSession != null) {
            sender.sendComponent(
                Component.text("Ya hay una sesión activa. Usa /dragontraining stop primero.")
                    .color(NamedTextColor.RED)
            )
            return
        }

        val count = args.getOrNull(1)?.toIntOrNull()
        if (count == null || count < 1) {
            sender.sendComponent(
                Component.text("Uso: /dragontraining start <amount>")
                    .color(NamedTextColor.RED)
            )
            return
        }

        val spawnLocation = sender.location

        val pointGenerator = RandomTrainingPointGenerator(
            spawnLocation.world,
            spawnLocation,
            20.0,
            80.0,
            15.0,
            10.0,
            250.0,
            maxOf(count * 4, 32) // at least 4 unique targets per dragon
        )

        val session = DragonTrainingSession(
            plugin,
            count,
            spawnLocation,
            pointGenerator
        )

        try {
            session.start()
            activeSession = session

            sender.sendComponent(
                Component.text("✔ Sesión de entrenamiento iniciada con $count dragón(es).")
                    .color(NamedTextColor.GREEN)
            )
            sender.sendComponent(
                Component.text("  Spawn: ${formatLocation(spawnLocation)}")
                    .color(NamedTextColor.GRAY)
            )
            sender.sendComponent(
                Component.text("  Radio de targets: 20 – 80 bloques | Rango vertical: ±15 bloques")
                    .color(NamedTextColor.GRAY)
            )

        } catch (e: Exception) {
            sender.sendComponent(
                Component.text("Error al iniciar la sesión: ${e.message}").color(NamedTextColor.RED)
            )
            plugin.logger.severe("[DragonTrainingCommand] Session start failed: ${e.message}")
        }
    }

    /**
     * `/dragontraining stop`
     *
     * Stops the active session (triggers model save -> close inside the session).
     */
    private fun handleStop(sender: Player) {
        val session = activeSession
        if (session == null) {
            sender.sendComponent(
                Component.text("No hay ninguna sesión activa.").color(NamedTextColor.YELLOW)
            )
            return
        }

        session.stop()
        activeSession = null

        sender.sendComponent(
            Component.text("✔ Sesión detenida. Pesos del modelo guardados.")
                .color(NamedTextColor.GREEN)
        )
    }

    /**
     * `/dragontraining status`
     *
     * Prints live diagnostics about the active session and the underlying
     * DragonQueueManager without touching or restarting anything.
     */
    private fun handleStatus(sender: Player) {
        val session = activeSession

        if (session == null || !session.isRunning) {
            sender.sendComponent(
                Component.text("Estado: ").color(NamedTextColor.GRAY)
                    .append(Component.text("DETENIDA").color(NamedTextColor.RED))
            )
            return
        }

        val manager = session.manager

        sender.sendComponent(
            Component.text("━━ Dragon Training Status ━━").color(NamedTextColor.GOLD)
        )
        sender.sendComponent(
            Component.text("  Estado:        ").color(NamedTextColor.GRAY)
                .append(Component.text("ACTIVA").color(NamedTextColor.GREEN))
        )
        sender.sendComponent(
            Component.text("  Dragones:      ").color(NamedTextColor.GRAY)
                .append(Component.text("${session.activeDragonCount}").color(NamedTextColor.WHITE))
        )
        sender.sendComponent(
            Component.text("  Timeouts pend: ").color(NamedTextColor.GRAY)
                .append(Component.text("${session.pendingTimeoutCount}").color(NamedTextColor.WHITE))
        )
        sender.sendComponent(
            Component.text("  Queue depth:   ").color(NamedTextColor.GRAY)
                .append(Component.text("${manager.queueDepth}").color(NamedTextColor.WHITE))
        )
        sender.sendComponent(
            Component.text("  Procesados:    ").color(NamedTextColor.GRAY)
                .append(Component.text("${manager.totalProcessed}").color(NamedTextColor.WHITE))
        )
        sender.sendComponent(
            Component.text("  Ticks lentos:  ").color(NamedTextColor.GRAY)
                .append(
                    Component.text("${manager.slowTickCount}")
                        .color(if (manager.slowTickCount > 0) NamedTextColor.YELLOW else NamedTextColor.WHITE)
                )
        )

        // List each dragon UUID and its current target
        sender.sendComponent(
            Component.text("  Dragones activos:").color(NamedTextColor.GRAY)
        )
        session.activeDragons.forEach { dragon ->
            val targetStr = dragon.locationTarget
                .map { "%.1f, %.1f, %.1f".format(it.x, it.y, it.z) }
                .orElse("sin target")
            sender.sendComponent(
                Component.text("    • ${dragon.uuid} → $targetStr").color(NamedTextColor.DARK_GRAY)
            )
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {

        if (args.size == 1) {
            return listOf("start", "stop", "status")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].lowercase() == "start") {
            // Suggest sensible dragon counts as quick-picks
            return listOf("1", "2", "4", "8")
                .filter { it.startsWith(args[1]) }
        }

        return emptyList()
    }

    private fun sendUsage(sender: Player) {
        sender.sendComponent(
            Component.text("Uso: /dragontraining <start <cantidad>|stop|status>")
                .color(NamedTextColor.RED)
        )
    }

    private fun formatLocation(loc: org.bukkit.Location): String {
        return "%.1f, %.1f, %.1f (${loc.world.name})".format(loc.x, loc.y, loc.z)
    }
}