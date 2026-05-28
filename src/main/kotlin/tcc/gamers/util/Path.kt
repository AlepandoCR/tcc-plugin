package tcc.gamers.util

import org.bukkit.Location

enum class PathMode {
    CLOSEST_TO_FURTHEST,  // walk insertion order: first → last
    FURTHEST_TO_CLOSEST   // walk insertion order reversed: last → first
}

class Path(
    private var referenceLocation: Location? = null,
    private var mode: PathMode = PathMode.CLOSEST_TO_FURTHEST
) {

    private val locations: MutableList<Location> = ArrayList()
    private val queue: ArrayDeque<Location> = ArrayDeque()
    private var dirty: Boolean = true

    fun addLocation(newLocation: Location) {
        locations.add(newLocation)
        dirty = true
    }

    fun removeLocation(location: Location) {
        locations.remove(location)
        queue.remove(location)
    }

    fun clear() {
        locations.clear()
        queue.clear()
        dirty = true
    }

    // Reference location is kept for external readers (e.g. TaxiSpawner.selectAutoMode)
    // but no longer influences traversal order — insertion order is always used.
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
    fun getLocations(): List<Location> = locations.toList()

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