package tcc.gamers.tutorials.tutorial.session

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import net.kyori.adventure.text.Component
import tcc.gamers.util.timerRunnable
import java.time.Instant
import java.util.*
import tcc.gamers.util.hideAllExcept
import tcc.gamers.util.addTutorialTag
import tcc.gamers.tutorials.entities.HiddenPlayerManager
import tcc.gamers.util.sendActionBarComponent
import tcc.gamers.util.timer

/**
 * Base session implementation with helpers for spawning temporary visuals and lifecycle.
 */
abstract class AbstractSession(override val id: UUID, override val player: Player) : Session {
    protected val visuals: MutableList<Entity> = mutableListOf()
    protected var startedAt: Instant = Instant.now()
    protected var tickTask: BukkitRunnable? = null

    override fun start() {
        startedAt = Instant.now()
        // start a default tick task if subclass provides onTick by overriding startTick
        startTick()
    }

    /**
     * Start sending an action bar to the player periodically while the session is active.
     * The supplier is invoked on each tick to obtain the Component to send.
     */
    protected fun startActionBar(
        supplier: () -> Component,
        periodTicks: Long = 20L
    ) {
        stopActionBar()
        tickTask = timerRunnable {
            try {
                player.sendActionBarComponent(supplier())
            } catch (_: Exception) {}
        }
        tickTask?.timer(0, periodTicks)
    }

    protected fun stopActionBar() {
        try { tickTask?.cancel() } catch (_: Exception) {}
        tickTask = null
        try { player.sendActionBarComponent(Component.empty()) } catch (_: Exception) {}
    }

    protected open fun startTick() {
        // default: no periodic tick, subclasses may override
    }

    override fun stop(save: Boolean) {
        // default: cleanup resources
        cleanup()
    }

    protected fun spawnMarkerAt(location: org.bukkit.Location, tag: String = "sessionMarker"): Entity? {
        return try {
            val world = location.world ?: return null
            val spawnLoc = location.clone()
            // center the display
            spawnLoc.add(0.5, 0.5, 0.5)

            val entity = world.spawn(spawnLoc, org.bukkit.entity.BlockDisplay::class.java)
            // make marker glow so it's more noticeable to the editor
            try { entity.isGlowing = true } catch (_: Exception) {}
            entity.addTutorialTag(tag)
            entity.hideAllExcept(player)
            HiddenPlayerManager.add(entity)
            visuals.add(entity)
            entity
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    protected fun removeLastVisual() {
        val e = visuals.lastOrNull() ?: return
        try {
            e.remove()
        } catch (_: Exception) {
        }
        visuals.remove(e)
    }

    override fun cleanup() {
        // remove visuals and cancel tick
        try {
            visuals.forEach {
                try { it.remove() } catch (_: Exception) {}
            }
            visuals.clear()
        } catch (_: Exception) {}
        try {
            tickTask?.cancel()
        } catch (_: Exception) {}
    }
}





