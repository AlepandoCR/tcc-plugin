package tcc.gamers.tutorials.entities

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import tcc.gamers.TCCPlugin
import tcc.gamers.util.DynamicListener
import tcc.gamers.util.Manager
import tcc.gamers.util.hide
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object HiddenPlayerManager: Manager<Entity> {
    override val list = mutableListOf<Entity>(  )
    lateinit var dynamicListener: DynamicListener

    fun start(plugin: TCCPlugin){
        listener()
        dynamicListener = DynamicListener(plugin)
        dynamicListener.setListener(listener())
        dynamicListener.start()
    }

    fun stop(){
        if(::dynamicListener.isInitialized && dynamicListener.isPresent()){
            dynamicListener.stop()
        }
    }

    private fun listener(): Listener {
        val listener = object : Listener {



            @EventHandler
            fun onJoinEvent(e: PlayerJoinEvent) {
                updateHidden(e)
            }

            @EventHandler
            fun onEntityDespawn(e: EntityRemoveFromWorldEvent){
                val entity = e.entity
                if(list.contains(entity)){
                    remove(entity)
                }
            }
        }

        return listener
    }

    private fun updateHidden(e: PlayerJoinEvent) {
        list.forEach { it.hide(e.player) }
    }


}