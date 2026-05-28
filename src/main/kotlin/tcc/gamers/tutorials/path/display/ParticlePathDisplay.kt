package tcc.gamers.tutorials.path.display

import tcc.gamers.tutorials.path.PathDisplay
import tcc.gamers.util.distance
import tcc.gamers.util.timer
import tcc.gamers.util.timerRunnable
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class ParticlePathDisplay(private val particle: Particle, player: Player, location: Location, spacing: Int, private val goalLocation: Location) : PathDisplay(player, location,
    spacing
) {
    private lateinit var runnable: BukkitRunnable
    private fun spawn(location: Location){
        player.spawnParticle(particle,location,1,0.4,0.3,0.4,0.0001)
    }
    override fun start() {
        runnable = timerRunnable {
            if(distance(player.location,goalLocation) > distance(location,goalLocation)) spawn(location)
        }

        runnable.timer(0,0)
    }

    override fun cancel() {
        runnable.cancel()
    }
}