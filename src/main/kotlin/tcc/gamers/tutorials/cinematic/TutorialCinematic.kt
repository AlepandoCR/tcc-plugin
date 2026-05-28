package tcc.gamers.tutorials.cinematic

import tcc.gamers.tutorials.tutorial.data.CinematicDto
import tcc.gamers.util.timer
import tcc.gamers.util.timerRunnable
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import tcc.gamers.tutorials.tutorial.ui.display.ModelTutorialDisplay
import tcc.gamers.tutorials.tutorial.ui.display.TutorialDisplay
import java.lang.Math.toDegrees
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow

class TutorialCinematic(
    private val p1: Location,
    private val targetLocation: Location,
    private val tutorialDisplay: TutorialDisplay? = null,
    private val cinematicDto: CinematicDto? = null
) : Cinematic {


    private lateinit var gameMode:GameMode
    private lateinit var p2: Location
    private lateinit var p3: Location
    // initial and final look-at points used to compute orientation along the path
    private lateinit var initialLookPoint: Location
    private lateinit var finalLookPoint: Location

    private var playing = false
    private lateinit var task: BukkitRunnable
    private var speed = cinematicDto?.speed ?: 0.01
    private var durationSeconds = cinematicDto?.duration ?: 5

    fun setDuration(seconds: Int) {
        durationSeconds = seconds.coerceIn(1, 60)
        speed = 1.0 / (durationSeconds * 20.0)
        recalculateControlPoints()
    }

    private fun recalculateControlPoints() {
        // Compute control points biased by where the camera looks at the start and the end.
        // This avoids the old heuristic that often produced an upward curve.
        val baseDistance = p1.distance(targetLocation).coerceAtLeast(0.1)

        // Initial look point: use the initial location's direction (player's facing) projected forward
        val initialDir = p1.direction.clone()?.normalize()
            ?: targetLocation.toVector().subtract(p1.toVector()).normalize()
        initialLookPoint = p1.clone().add(initialDir.multiply(baseDistance))

        // Final look point: either provided by the cinematic DTO or default to initial look (no change)
        finalLookPoint = cinematicDto?.lookAt?.build(p1.world) ?: initialLookPoint.clone()

        // Forward direction from start to target
        val forward = targetLocation.toVector().subtract(p1.toVector()).normalize()

        // Bias directions for control points: prefer horizontal components to avoid up-curve
        fun horizontal(v: Vector): Vector {
            val hv = v.clone()
            val y = hv.y
            hv.y = 0.0
            if (hv.length() < 1e-6) {
                // fallback to a small vertical-preserving vector
                return Vector(0.0, y * 0.2, 0.0)
            }
            return hv.normalize()
        }

        val initBias = initialLookPoint.toVector().subtract(p1.toVector()).normalize()
        val finalBias = finalLookPoint.toVector().subtract(targetLocation.toVector()).normalize()

        val initH = horizontal(initBias)
        val finalH = horizontal(finalBias)

        // distances for forward/back offsets and side bias
        val forwardOffset = baseDistance * 0.25
        val sideScale = baseDistance * 0.6

        p2 = p1.clone()
            .add(forward.clone().multiply(forwardOffset))
            .add(initH.clone().multiply(sideScale))
        p3 = targetLocation.clone()
            .subtract(forward.clone().multiply(forwardOffset))
            .add(finalH.clone().multiply(sideScale))

        // Add a small vertical influence from the original look directions so vertical changes are subtle
        p2.y = p1.y + (initBias.y * baseDistance * 0.2)
        p3.y = targetLocation.y + (finalBias.y * baseDistance * 0.2)
    }

    private fun bezier(t: Double): Location {
        val x = (1 - t).pow(2) * (1 - t) * p1.x +
                3 * (1 - t).pow(2) * t * p2.x +
                3 * (1 - t) * t.pow(2) * p3.x +
                t.pow(3) * targetLocation.x

        val y = (1 - t).pow(2) * (1 - t) * p1.y +
                3 * (1 - t).pow(2) * t * p2.y +
                3 * (1 - t) * t.pow(2) * p3.y +
                t.pow(3) * targetLocation.y

        val z = (1 - t).pow(2) * (1 - t) * p1.z +
                3 * (1 - t).pow(2) * t * p2.z +
                3 * (1 - t) * t.pow(2) * p3.z +
                t.pow(3) * targetLocation.z

        return Location(p1.world, x, y, z)
    }

    private fun bezierCurveAnimation(player: Player) {
        playing = true
        var t = 0.0

        task = timerRunnable {
            if (t > 1.0) {
                stop(player)
                return@timerRunnable
            }

            val loc = bezier(t)
            // Interpolate the look-at point from initialLookPoint to finalLookPoint so
            // the camera smoothly rotates toward the final look target along the path.
            val lookVec = initialLookPoint.toVector().multiply(1.0 - t).add(finalLookPoint.toVector().multiply(t))
            val dir = lookVec.subtract(loc.toVector())
            val dirLength = dir.length()

            val yaw = if (dirLength > 0) toDegrees(atan2(-dir.x, dir.z)).toFloat() else 0f
            val pitch = if (dirLength > 0.01) {
                toDegrees(-asin((dir.y / dirLength).coerceIn(-1.0, 1.0))).toFloat()
            } else {
                0f
            }

            loc.yaw = yaw
            loc.pitch = pitch

            player.teleport(loc)

            t += speed
        }

        task.timer(0L, 0)
    }

    override fun play(player: Player) {
        if (playing) return
        gameMode = player.gameMode
        player.addTutorial()
        changePlayerGameMode(GameMode.SPECTATOR, player)
        if (!::p2.isInitialized || !::p3.isInitialized) recalculateControlPoints()
        bezierCurveAnimation(player)
        tutorialDisplay?.let {
            if(it is ModelTutorialDisplay){
                it.orbit(p1)
            }
        }
    }

    override fun stop(player: Player) {
        if (!playing) return
        player.teleport(p1)
        playing = false
        cancelTask()
        setGamemode(player)
        tutorialDisplay?.let {
            if(it is ModelTutorialDisplay){
                it.resetOrbitLocation()
            }
        }
        player.removeTutorial()
    }

    private fun setGamemode(player: Player) {
        var finalGameMode: GameMode = GameMode.ADVENTURE
        if (::gameMode.isInitialized) {
            finalGameMode = gameMode
        }
        changePlayerGameMode(finalGameMode, player)
    }

    private fun cancelTask() {
        if (::task.isInitialized)
            task.cancel()
    }

    override fun isPlaying(): Boolean {
        return playing
    }

    private fun changePlayerGameMode(gameMode: GameMode, player: Player) {
        player.gameMode = gameMode
    }

    companion object{
        private val tutorialPlayers = mutableListOf<Player>()

        private fun Player.addTutorial(){
            tutorialPlayers.add(this)
        }

        private fun Player.removeTutorial(){
            tutorialPlayers.remove(this)
        }

        fun Player.isOnCinematic(): Boolean{
            return tutorialPlayers.contains(this)
        }
    }
}
