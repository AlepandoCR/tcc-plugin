package tcc.gamers.util.path

import org.bukkit.Location

abstract class AbstractPath {

    protected val heightThreshold = 5
    protected val locations: MutableList<Location> = ArrayList()

    open fun addLocation(newLocation: Location) {
        locations.add(newLocation)
    }

    open fun removeLocation(location: Location) {
        locations.remove(location)
    }

    open fun clear() {
        locations.clear()
    }

    fun getLocationList(): List<Location> = locations.toList()

    fun isAirPath(): Boolean {
        return locations.any { location ->
            val world = location.world ?: return@any false
            val highestY = world.getHighestBlockYAt(location)
            
            location.y > (highestY + heightThreshold)
        }
    }
}