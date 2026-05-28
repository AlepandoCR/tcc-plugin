package tcc.gamers.tutorials.tutorial.data

import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * DTO to represent a Location in YAML/JSON format.
 * Contains world, coordinates, yaw and pitch.
 */
data class PathLocationDto(
    var world: String = "",      // world name
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var yaw: Float = 0f,
    var pitch: Float = 0f
) {
    /**
     * Converts this DTO to a Bukkit Location.
     * Returns null if the world does not exist.
     */
    fun toLocation(): Location? {
        val bukkitWorld = Bukkit.getWorld(world) ?: return null
        return Location(bukkitWorld, x, y, z, yaw, pitch)
    }

    companion object {
        /**
         * Creates a PathLocationDto from a Bukkit Location.
         */
        fun fromLocation(location: Location): PathLocationDto {
            return PathLocationDto(
                world = location.world?.name ?: "world",
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            )
        }
    }
}


