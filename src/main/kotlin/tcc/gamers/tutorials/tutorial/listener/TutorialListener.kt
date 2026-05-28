package tcc.gamers.tutorials.tutorial.listener

import tcc.gamers.tutorials.cinematic.TutorialCinematic.Companion.isOnCinematic
import tcc.gamers.tutorials.events.TutorialEvent
import tcc.gamers.tutorials.events.TutorialPlayerEvent
import tcc.gamers.tutorials.events.types.PlayerGetToLocationEvent
import tcc.gamers.tutorials.tutorial.data.GetToLocationDto
import tcc.gamers.util.timer
import tcc.gamers.util.timerRunnable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import tcc.gamers.tutorials.tutorial.TutorialManager

class TutorialListener<T : TutorialEvent>(
    private val player: Player,
    val event: Class<T>,
    private val getToLocationDto: GetToLocationDto? = null
) : Listener {

    var completed = false
    lateinit var run: BukkitRunnable
    val world = player.world
    init {

        getToLocationDto?.let {
            run = timerRunnable {
                if (it.isNear(player) && !player.isOnCinematic()){

                    PlayerGetToLocationEvent(
                        player,
                        it.location!!
                            .build(world)
                    ).callEvent()

                    cancelGetToLocation()
                }

            }
            run.timer(0,0)
        }
    }

    @EventHandler
    fun onEvent(event: T){
        processEvent(event)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent){
        // cancel if player quits
        TutorialManager.cancel(event.player)
    }

    private fun processEvent(event: T){
        if (event::class.java != this.event) {
            return
        }

        if(event is PlayerGetToLocationEvent){
            if(event.destination != getToLocationDto?.location?.build(world) ){
                Bukkit.getLogger().info("Rejecting event due to location filter")
                return
            }
        }

        processPlayerEvent(event)

    }

    private fun processPlayerEvent(event: T) {
        val isTutorialPlayerEvent = event is TutorialPlayerEvent

        if(!isTutorialPlayerEvent) return

        val eventPlayer: Player = (event as TutorialPlayerEvent).player

        if (eventPlayer == player) {
            cancelGetToLocation()
            completed = true
        }
    }

    private fun cancelGetToLocation() {
        if (::run.isInitialized && !run.isCancelled) {
            run.cancel()
        }
    }
}