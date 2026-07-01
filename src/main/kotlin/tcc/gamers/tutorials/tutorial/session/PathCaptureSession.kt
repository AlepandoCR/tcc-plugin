package tcc.gamers.tutorials.tutorial.session

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import tcc.gamers.tutorials.tutorial.manager.PathManager
import tcc.gamers.util.path.Path
import tcc.gamers.util.path.PathMode
import tcc.gamers.util.sendComponent
import java.util.*

/**
 * Interactive session for capturing/editing a Path in-game.
 * Right click -> add point
 * Left click -> remove last point
 * Shift + Left click -> finish and save
 */
class PathCaptureSession(
    override val id: UUID,
    override val player: Player,
    private val pathId: String,
    private val pathManager: PathManager,
    private val plugin: tcc.gamers.TCCPlugin,
    private val editExisting: Boolean = false,
    @Suppress("unused") private val mode: PathMode = PathMode.CLOSEST_TO_FURTHEST
) : AbstractSession(id, player) {

    private val path: Path = Path(player.location, mode)
    private var saved = false
    private var lastInteractionTime: Long = 0L
    private val INTERACTION_COOLDOWN_MS: Long = 200L  // 200ms cooldown between interactions

    override fun start() {
        super.start()
        // if editing existing path, load it
        if (editExisting) {
            val loaded = pathManager.loadPath(pathId, player.location)
            if (loaded != null) {
                // copy loaded locations into our path
                loaded.getLocationList().forEach { path.addLocation(it) }
                // spawn markers for existing points
                spawnMarkersForPath()
                player.sendComponent(Component.text("Editing path '$pathId' with ${path.getLocationList().size} points.").color(NamedTextColor.YELLOW))
            } else {
                player.sendComponent(Component.text("Path '$pathId' not found; starting new.").color(NamedTextColor.RED))
            }
        } else {
            player.sendComponent(Component.text("Creating new path '$pathId'. Right click: add point, Left click: remove last, Shift+Left: finish and save.").color(NamedTextColor.GREEN))
        }
        // start action bar updater
        startActionBar({ Component.text("Path editor: Right click to add, Left click to remove, Shift+Left to finish").color(NamedTextColor.AQUA) }, 10L)
    }

    override fun lockingResourceId(): String {
        return pathId
    }

    private fun spawnMarkersForPath() {
        path.getLocationList().forEach { loc ->
            spawnMarkerAt(loc, "pathCapture:$pathId")
        }
    }

    override fun handlePlayerInteract(event: PlayerInteractEvent) {
        if (event.player.uniqueId != player.uniqueId) return

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInteractionTime < INTERACTION_COOLDOWN_MS) {
            return  // Still on cooldown, ignore
        }
        lastInteractionTime = currentTime

        val action = event.action
        // Right click: add point (use clicked block if present else eye target)
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            event.isCancelled = true
            val point = determinePointFromEvent(event)
            addPoint(point)
            return
        }

        // Left click: remove last
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            // shift + left -> finish and save
            if (player.isSneaking) {
                event.isCancelled = true
                finishAndSave()
                return
            }
            event.isCancelled = true
            removeLastPoint()
            return
        }
    }

    private fun determinePointFromEvent(event: PlayerInteractEvent): Location {
        val clicked = event.clickedBlock
        return if (clicked != null) {
            clicked.location.add(0.5, 1.0, 0.5) // put marker above block center
        } else {
            // fallback to player's eye target 5 blocks ahead
            val eye = player.eyeLocation
            eye.add(eye.direction.multiply(3.0))
        }
    }

    fun addPoint(location: Location) {
        path.addLocation(location)
        // spawn marker on main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            spawnMarkerAt(location, "pathCapture:$pathId")
        })
        player.sendComponent(Component.text("Point added. Total: ${path.getLocationList().size}").color(NamedTextColor.GREEN))
    }

    fun removeLastPoint() {
        val locs = path.getLocationList()
        if (locs.isEmpty()) {
            player.sendComponent(Component.text("No points to remove.").color(NamedTextColor.YELLOW))
            return
        }
        // remove last location from Path (Path class has removeLocation(location))
        val last = locs.last()
        path.removeLocation(last)
        // remove last visual
        Bukkit.getScheduler().runTask(plugin, Runnable {
            removeLastVisual()
        })
        player.sendComponent(Component.text("Removed last point. Remaining: ${path.getLocationList().size}").color(NamedTextColor.YELLOW))
    }

    private fun finishAndSave() {
        // save path
        val ok = pathManager.savePath(pathId, path)
        if (ok) {
            player.sendComponent(Component.text("Path '$pathId' saved (${path.getLocationList().size} points).").color(NamedTextColor.GOLD))
            saved = true
        } else {
            player.sendComponent(Component.text("Failed to save path '$pathId'.").color(NamedTextColor.RED))
        }
        // stop the session and cleanup
        stop(save = true)
    }

    override fun handleQuit() {
        // no auto-save by default, just cleanup
        stopActionBar()
        cleanup()
    }

    override fun stop(save: Boolean) {
        super.stop(save)
        // if not saved and save==true, try saving
        if (!saved && save) {
            pathManager.savePath(pathId, path)
        }
        stopActionBar()
        cleanup()
    }
}




