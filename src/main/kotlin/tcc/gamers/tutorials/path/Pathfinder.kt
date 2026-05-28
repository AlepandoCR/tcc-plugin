package tcc.gamers.tutorials.path

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation
import net.minecraft.world.level.pathfinder.Path
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftMob
import org.bukkit.entity.Silverfish

class Pathfinder(
    private val world: World,
    private val start: Location,
    private val goal: Location,
    private val distance: Int
) {
    private val list = mutableListOf<Location>()
    private var steps = 0
    fun findPath(): List<Location> {
        createPath(start)
        return list
    }

    private fun createPath(start: Location) {
        val entity = world.spawn(start.add(0.0,-2.0,0.0), Silverfish::class.java)
        entity.isInvisible = true
        val other = entity.copy()
        other.addPassenger(entity)
        val nmsEntity = (entity as CraftMob).handle
        val blockPos = BlockPos(goal.x.toInt(), goal.y.toInt(), goal.z.toInt())
        val nav = GroundPathNavigation(nmsEntity, (world as CraftWorld).handle)
        val path = nav.createPath(blockPos, 0)

        addLocationsToList(path)
        other.remove()
        entity.remove()

        if(list.isNotEmpty() && list.last().distance(goal) > distance && steps < 3){
            steps++
            createPath(list.last())
        }
    }

    private fun addLocationsToList(path: Path?) {
        path?.let { p ->
            p.nodes.forEach {
                val location = Location(world, it.x.toDouble(), it.y.toDouble(), it.z.toDouble())
                list.add(location)
            }
        }
    }
}