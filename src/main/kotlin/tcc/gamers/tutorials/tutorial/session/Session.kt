package tcc.gamers.tutorials.tutorial.session

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

/**
 * Generic session interface for interactive in-game sessions.
 * Implementations should be lightweight and let SessionManager handle event dispatching.
 */
interface Session {
    val id: UUID
    val player: Player

    fun start()
    fun stop(save: Boolean = true)

    /**
     * Handle player interact events delegated by the centralized dispatcher.
     */
    fun handlePlayerInteract(event: PlayerInteractEvent)

    /**
     * Called when player quits or otherwise needs session cleanup.
     */
    fun handleQuit()

    /**
     * Cleanup any spawned visuals/resources. Called by SessionManager when stopping.
     */
    fun cleanup()

    /**
     * Optional resource id that this session locks while active (e.g. a path id).
     * Return null for no locking.
     */
    @Suppress("unused")
    fun lockingResourceId(): String? = null
}


