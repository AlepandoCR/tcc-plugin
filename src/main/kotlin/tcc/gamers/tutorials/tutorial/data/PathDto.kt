package tcc.gamers.tutorials.tutorial.data

import tcc.gamers.util.Path
import tcc.gamers.util.PathMode
import org.bukkit.Location

/**
 * DTO to represent a complete Path in YAML/JSON format.
 * Contains a unique ID, traversal mode, reference location and ordered list of locations.
 */
data class PathDto(
    var id: String = "",                                    // unique path ID
    var mode: String = PathMode.CLOSEST_TO_FURTHEST.name,  // traversal mode (serialized as String)
    var referenceLocation: PathLocationDto? = null,         // reference location (optional)
    var locations: MutableList<PathLocationDto> = mutableListOf()  // ordered list of locations
) {
    /**
     * Converts this DTO to a functional Path object.
     */
    fun toPath(defaultLocation: Location? = null): Path {
        val refLocation = referenceLocation?.toLocation() ?: defaultLocation
        val pathMode = try {
            PathMode.valueOf(this.mode)
        } catch (_: IllegalArgumentException) {
            PathMode.CLOSEST_TO_FURTHEST  // fallback if mode is invalid
        }

        val path = Path(refLocation, pathMode)
        for (locDto in locations) {
            locDto.toLocation()?.let { path.addLocation(it) }
        }
        return path
    }

    companion object {
        /**
         * Creates a PathDto from a Path object.
         * Note: Locations in Path are kept in their current order (live list).
         * This DTO takes a "snapshot" of the current state.
         */
        fun fromPath(id: String, path: Path): PathDto {
            val refLoc = path.getReferenceLocation()?.let { PathLocationDto.fromLocation(it) }
            val locs = path.getLocations().map { PathLocationDto.fromLocation(it) }.toMutableList()

            return PathDto(
                id = id,
                mode = path.getMode().name,
                referenceLocation = refLoc,
                locations = locs
            )
        }
    }
}


