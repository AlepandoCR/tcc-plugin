package tcc.gamers.tutorials.tutorial

import tcc.gamers.util.Manager
import org.bukkit.entity.Player

object TutorialManager: Manager<Tutorial> {
    override val list: MutableList<Tutorial> = mutableListOf()

    fun cancelAll(){
        list.forEach {
            it.cancel(true)
        }
    }

    fun cancel(player: Player){
        list.firstOrNull { it.player == player }?.cancel(true)
    }
}