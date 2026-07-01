package tcc.gamers.tutorials.tutorial.session

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.manager.RacePathManager
import tcc.gamers.util.path.PathMode
import tcc.gamers.util.path.RacePath
import tcc.gamers.util.sendComponent
import java.util.*

/**
 * Interactive session for capturing/editing a RacePath in-game.
 * Right click -> add point
 * Left click -> remove last point
 * Shift + Left click -> finish and save
 */
class RacePathCaptureSession(
    override val id: UUID,
    override val player: Player,
    private val pathId: String,
    private val racePathManager: RacePathManager,
    private val plugin: TCCPlugin,
    private val editExisting: Boolean = false,
) : AbstractSession(id, player) {

    private val path: RacePath = RacePath()
    private var saved = false
    private var lastInteractionTime: Long = 0L
    private val INTERACTION_COOLDOWN_MS: Long = 200L

    override fun start() {
        super.start()
        
        if (editExisting) {
            racePathManager.loadPathAsync(pathId).thenAccept { loaded ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (loaded != null) {
                        loaded.getLocationList().forEach { path.addLocation(it) }
                        spawnMarkersForPath()
                        player.sendComponent(Component.text("Editing race path '$pathId' with ${path.getLocationList().size} points.").color(NamedTextColor.YELLOW))
                    } else {
                        player.sendComponent(Component.text("Race path '$pathId' not found; starting new.").color(NamedTextColor.RED))
                    }
                })
            }
        } else {
            player.sendComponent(Component.text("Creating new race path '$pathId'. Right click: add point, Left click: remove last, Shift+Left: finish and save.").color(NamedTextColor.GREEN))
        }
        
        startActionBar({ Component.text("RacePath editor: Right click to add, Left click to remove, Shift+Left to finish").color(NamedTextColor.AQUA) }, 10L)
    }

    override fun lockingResourceId(): String {
        return pathId
    }

    private fun spawnMarkersForPath() {
        path.getLocationList().forEach { loc ->
            spawnMarkerAt(loc, "racePathCapture:$pathId")
        }
    }

    override fun handlePlayerInteract(event: PlayerInteractEvent) {
        if (event.player.uniqueId != player.uniqueId) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInteractionTime < INTERACTION_COOLDOWN_MS) {
            return
        }
        lastInteractionTime = currentTime

        val action = event.action
        
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            event.isCancelled = true
            val point = determinePointFromEvent(event)
            addPoint(point)
            return
        }

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
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
            clicked.location.add(0.5, 1.0, 0.5) 
        } else {
            val eye = player.eyeLocation
            eye.add(eye.direction.multiply(3.0))
        }
    }

    fun addPoint(location: Location) {
        path.addLocation(location)
        Bukkit.getScheduler().runTask(plugin, Runnable {
            spawnMarkerAt(location, "racePathCapture:$pathId")
        })
        player.sendComponent(Component.text("Point added. Total: ${path.getLocationList().size}").color(NamedTextColor.GREEN))
    }

    fun removeLastPoint() {
        val locs = path.getLocationList()
        if (locs.isEmpty()) {
            player.sendComponent(Component.text("No points to remove.").color(NamedTextColor.YELLOW))
            return
        }
        
        val last = locs.last()
        path.removeLocation(last)
        
        Bukkit.getScheduler().runTask(plugin, Runnable {
            removeLastVisual()
        })
        player.sendComponent(Component.text("Removed last point. Remaining: ${path.getLocationList().size}").color(NamedTextColor.YELLOW))
    }

    private fun finishAndSave() {
        racePathManager.savePathAsync(pathId, path).thenAccept { ok ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (ok) {
                    player.sendComponent(Component.text("Race path '$pathId' saved (${path.getLocationList().size} points).").color(NamedTextColor.GOLD))
                    saved = true
                } else {
                    player.sendComponent(Component.text("Failed to save race path '$pathId'.").color(NamedTextColor.RED))
                }
                stop(save = false)
            })
        }

        plugin.raceManager.reload()
    }

    override fun handleQuit() {
        stopActionBar()
        cleanup()
    }

    override fun stop(save: Boolean) {
        super.stop(save)
        
        if (!saved && save) {
            racePathManager.savePathAsync(pathId, path).thenAccept { ok ->
                if (ok) saved = true
            }
        }
        
        stopActionBar()
        cleanup()
    }
}