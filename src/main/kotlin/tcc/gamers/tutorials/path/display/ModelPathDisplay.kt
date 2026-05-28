package tcc.gamers.tutorials.path.display

import tcc.gamers.tutorials.entities.HiddenPlayerManager
import tcc.gamers.tutorials.model.Model
import tcc.gamers.tutorials.path.PathDisplay
import tcc.gamers.util.hideAllExcept
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.atan2

class ModelPathDisplay(private val model: Model<out Entity>, player: Player, location: Location, private val nextPoint: Location? = null,
                       spacing: Int
) : PathDisplay(player, location, spacing) {
    override fun start() {
        HiddenPlayerManager.add(model.entity)
        model.entity.hideAllExcept(player)
        model.teleport(location)
        rotateModel(nextPoint)
    }

    private fun rotateModel(nextPoint: Location?) {
        nextPoint?.let {
            val dir = nextPoint.clone().subtract(location)
            dir.y = 0.0
            if (dir.lengthSquared() > 0) {
                val yaw = vectorToYaw(dir.toVector())
                model.rotate(yaw)
            }
        }
    }

    override fun cancel() {
        model.despawn()
    }

    private fun vectorToYaw(vector: Vector): Float {
        return Math.toDegrees(atan2(-vector.x, vector.z)).toFloat()
    }
}