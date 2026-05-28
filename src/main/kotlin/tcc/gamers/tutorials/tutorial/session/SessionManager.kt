package tcc.gamers.tutorials.tutorial.session

import tcc.gamers.TCCPlugin
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import java.util.*

/**
 * Session manager: tracks active sessions and registers the central event dispatcher.
 */
class SessionManager(@Suppress("unused") private val plugin: TCCPlugin) {
    private val sessionsByPlayer: MutableMap<UUID, Session> = HashMap()
    private val dispatcher = SessionEventDispatcher(this)
    // resource locks (resourceId -> owner UUID)
    private val resourceLocks: MutableMap<String, UUID> = HashMap()

    init {
        Bukkit.getPluginManager().registerEvents(dispatcher, plugin)
    }

    fun startSession(session: Session): Boolean {
        val playerId = session.player.uniqueId
        if (sessionsByPlayer.containsKey(playerId)) return false
        // handle resource locking if session declares a resource id
        val resourceId = session.lockingResourceId()
        if (resourceId != null) {
            val owner = resourceLocks[resourceId]
            if (owner != null) return false
            resourceLocks[resourceId] = playerId
        }
        sessionsByPlayer[playerId] = session
        session.start()
        return true
    }

    fun getSessionForPlayer(player: Player): Session? {
        return sessionsByPlayer[player.uniqueId]
    }

    fun stopSessionForPlayer(player: Player, save: Boolean = true) {
        val session = sessionsByPlayer.remove(player.uniqueId) ?: return
        try {
            session.stop(save)
        } finally {
            // unlock resource if any
            session.lockingResourceId()?.let { resourceId ->
                resourceLocks.remove(resourceId)
            }
            session.cleanup()
        }
    }

    fun stopAll() {
        val copy = sessionsByPlayer.values.toList()
        for (s in copy) {
            try {
                s.stop(false)
            } catch (_: Exception) {}
            try { s.cleanup() } catch (_: Exception) {}
        }
        sessionsByPlayer.clear()
    }

    @Suppress("unused")
    fun listActiveSessions(): List<Session> = sessionsByPlayer.values.toList()
}





