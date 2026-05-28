package tcc.gamers.tutorials.api

import tcc.gamers.TCCPlugin
import tcc.gamers.util.runTutorial
import org.bukkit.entity.Player

object TutorialAPI {
    fun startTutorial(player: Player,id: String){
        player.runTutorial(id, TCCPlugin.instance)
    }
}