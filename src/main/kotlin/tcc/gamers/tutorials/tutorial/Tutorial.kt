package tcc.gamers.tutorials.tutorial

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.events.TutorialEvent
import tcc.gamers.tutorials.events.types.PlayerCompleteTutorialEvent
import tcc.gamers.tutorials.tutorial.data.TutorialDto
import tcc.gamers.util.DynamicListener
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import tcc.gamers.tutorials.tutorial.steps.TutorialStep
import tcc.gamers.tutorials.tutorial.steps.queue.StepQueue
import tcc.gamers.tutorials.tutorial.ui.TutorialScoreboardManager

abstract class Tutorial(val player: Player, protected val plugin: TCCPlugin, val dto: TutorialDto, fileName: String) {
    private val gameMode = player.gameMode
    val stepQueue = StepQueue(plugin)
    val name by lazy { title() }
    val world = player.world
    val location = player.location
    private lateinit var scoreboardManager: TutorialScoreboardManager
    private val dynamicListener = DynamicListener(plugin)

    init {
        dto.world = player.world.name
    }

    fun cancel(resetLocation: Boolean){
        dynamicListener.stop()
        stepQueue.cancel()
        scoreboardManager.stop()
        TutorialManager.remove(this)
        player.gameMode = gameMode
        dto.steps = stepQueue.dtoList()
        if(resetLocation){
            player.teleport(location)
        }
    }

    fun complete(canceled: Boolean) {
        cancel(canceled)
        val title = completion()

        if (!canceled) {
            PlayerCompleteTutorialEvent(player, this).callEvent()
        }

        title?.let {
            player.showTitle(title)
        }

    }

    fun start(){
        TutorialManager.add(this)
        dynamicListener.setListener(TutorialHandler())
        dynamicListener.start()
        startScoreboard()
        loadSteps()
        stepQueue.cycleSteps(this)
    }

    private fun startScoreboard() {
        scoreboardManager = TutorialScoreboardManager(plugin, stepQueue, player, this)
    }

    abstract fun registerSteps(): List<TutorialStep<out TutorialEvent>>

    abstract fun title(): String

    abstract fun completion(): Title?

    private fun addStep(step: TutorialStep<out TutorialEvent>){
        stepQueue.addStep(step)
    }

    private fun loadSteps(){
        registerSteps().forEach{addStep(it)}
    }

    private inner class TutorialHandler : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            if (event.player == player) {
                if (islandTutorial()) return
                cancel(true)
            }
        }

        private fun islandTutorial(): Boolean {
            dto.checkActivity()
            if (dto.active && dto.islandTutorial) {
                cancel(false)
                return true
            }
            return false
        }

        @EventHandler
        fun onDeath(event: PlayerDeathEvent) {
            if (event.entity == player) {
                if (islandTutorial()) return
                cancel(true)
            }
        }

        @EventHandler
        fun onWorldChange(event: PlayerChangedWorldEvent) {
            if (event.player == player) {
                cancel(true)
            }
        }
    }
}