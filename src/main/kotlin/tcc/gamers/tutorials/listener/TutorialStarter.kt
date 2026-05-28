package tcc.gamers.tutorials.listener

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.TutorialManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent

class TutorialStarter(private val plugin: TCCPlugin) : Listener {
    @EventHandler
    fun playerWorldEvent(e: PlayerChangedWorldEvent) {
        cancel(e)
    }

    private fun cancel(e: PlayerChangedWorldEvent) {
        val player = e.player
        TutorialManager.cancel(player)
    }


}