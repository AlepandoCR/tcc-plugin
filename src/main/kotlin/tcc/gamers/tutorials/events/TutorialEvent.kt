package tcc.gamers.tutorials.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class TutorialEvent: Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}