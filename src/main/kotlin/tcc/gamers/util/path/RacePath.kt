package tcc.gamers.util.path

import org.bukkit.Location

class RacePath : AbstractPath() {

    fun getClosestCheckpointIndex(location: Location): Int {
        if (locations.isEmpty() || location.world == null) return -1

        var closestIndex = -1
        var minDistanceSquared = Double.MAX_VALUE

        for ((index, loc) in locations.withIndex()) {
            if (loc.world != location.world) continue

            val distanceSquared = loc.distanceSquared(location)
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared
                closestIndex = index
            }
        }
        
        return closestIndex
    }

    fun getDistanceToCheckpoint(location: Location, index: Int): Double {
        val checkpoint = getCheckpointLocation(index) ?: return -1.0
        
        if (checkpoint.world != location.world) return -1.0
        
        return location.distance(checkpoint)
    }

    fun getCheckpointLocation(index: Int): Location? {
        return locations.getOrNull(index)
    }

    fun isLastCheckpoint(index: Int): Boolean = index == locations.size - 1
}