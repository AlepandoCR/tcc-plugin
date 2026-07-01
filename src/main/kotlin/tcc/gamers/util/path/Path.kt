package tcc.gamers.util.path

import org.bukkit.Location

class Path(
    private var referenceLocation: Location? = null,
    private var mode: PathMode = PathMode.CLOSEST_TO_FURTHEST
) : AbstractPath() {

    private val queue: ArrayDeque<Location> = ArrayDeque()
    private var dirty: Boolean = true

    override fun addLocation(newLocation: Location) {
        super.addLocation(newLocation)
        dirty = true
    }

    override fun removeLocation(location: Location) {
        super.removeLocation(location)
        queue.remove(location)
    }

    override fun clear() {
        super.clear()
        queue.clear()
        dirty = true
    }

    fun setReferenceLocation(location: Location) {
        referenceLocation = location
    }

    fun setMode(pathMode: PathMode) {
        if (mode != pathMode) {
            mode = pathMode
            dirty = true
        }
    }

    /** Returns the next waypoint without consuming it. */
    fun peekNext(): Location? {
        ensureCommitted()
        return queue.firstOrNull()
    }

    /** Consumes and returns the next waypoint, or null when the path is exhausted. */
    fun next(): Location? {
        ensureCommitted()
        return queue.removeFirstOrNull()
    }

    fun getMode(): PathMode = mode
    fun getReferenceLocation(): Location? = referenceLocation

    private fun ensureCommitted() {
        if (!dirty) return
        queue.clear()
        when (mode) {
            PathMode.CLOSEST_TO_FURTHEST -> queue.addAll(locations)
            PathMode.FURTHEST_TO_CLOSEST -> queue.addAll(locations.reversed())
        }
        dirty = false
    }
}