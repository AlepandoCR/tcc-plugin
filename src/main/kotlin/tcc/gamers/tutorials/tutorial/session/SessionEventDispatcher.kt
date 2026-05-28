package tcc.gamers.tutorials.tutorial.session

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Central event dispatcher that delegates relevant events to the active session for the player.
 * SessionManager should register this listener.
 */
class SessionEventDispatcher(private val manager: SessionManager) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val session = manager.getSessionForPlayer(event.player)
        session?.handlePlayerInteract(event)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val session = manager.getSessionForPlayer(event.player) ?: return
        session.handleQuit()
        manager.stopSessionForPlayer(event.player, save = false)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        // noop for now, but sessions can handle if needed
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        // noop for now
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // noop
    }
}

