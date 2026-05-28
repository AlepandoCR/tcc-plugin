package tcc.gamers.tutorials.path

import tcc.gamers.tutorials.path.display.ParticlePathDisplay
import tcc.gamers.tutorials.tutorial.ui.display.Display
import tcc.gamers.util.timer
import tcc.gamers.util.timerRunnable
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class PathPointGenerator(private var start: Location, private val goal: Location, private val distance: Int,private val player: Player): Display {

    private val displays = mutableListOf<PathDisplay>()

    private lateinit var runnable: BukkitRunnable

    private fun generateDisplays() {
        val points = generateList()

        if (points.isEmpty()) {
            return
        }

        var lastPlaced: Location? = null

        points.forEach {
            val display = ParticlePathDisplay(Particle.WAX_ON,player,it,0, goal)

            if (lastPlaced == null || lastPlaced.distance(it) >= display.spacing) {
                display.start()
                displays.add(display)
                lastPlaced = it
            }
        }
    }

    private fun updater(){
        runnable = timerRunnable {
            if (player.location != start) {
                start = player.location
                updatePath()
            }
        }

        runnable.timer(0,0)
    }

    private fun updatePath(){
        clear()
        generateDisplays()
    }

    override fun start(){
        updater()
        generateDisplays()
    }

    override fun cancel(){
        if(::runnable.isInitialized){
            runnable.cancel()
        }
        clear()
    }

    private fun clear() {
        displays.forEach { it.cancel() }
        displays.clear()
    }

    private fun generateList(): List<Location> {
        return Pathfinder(start.world,start,goal,distance).findPath()
    }

}
