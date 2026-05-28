package tcc.gamers.tutorials.tutorial.ui.display

import tcc.gamers.tutorials.tutorial.data.DisplayDto
import tcc.gamers.tutorials.tutorial.data.TitleDto
import tcc.gamers.util.async
import tcc.gamers.util.sync
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.function.Supplier

class ModelTutorialDisplay(
    player: Player,
    target: Supplier<Location>,
    private val orbitTarget: Location,
    displayText: TitleDto? = null,
    models: DisplayDto? = null
) : TutorialDisplay(
    player,
    target,
    displayText,
    models
) {
    private val radius = 2.0
    private var orbitLocation = player.location
    var targetingPlayer = true
    // Track last player location to detect movement direction
    private var lastPlayerLocation = player.location.clone()

    fun resetOrbitLocation(){
        orbitLocation = player.location
        targetingPlayer = true
    }

    fun orbit(location: Location){
        orbitLocation = location
        targetingPlayer = false
    }

    override fun tick() {
        val targetLocation = target.get()
        val finalTargetLocation = targetLocation.clone().add(0.0,1.5,0.0)
        model?.teleport(finalTargetLocation)

        // Determine player's movement vector for orientation logic
        val currentPlayerLoc = player.location.clone()
        val movementVec = currentPlayerLoc.toVector().subtract(lastPlayerLocation.toVector())
        val movementDist = movementVec.length()

        // Walking model follows the PLAYER (like flyingModel) but stuck to ground and oriented
        walkingModel?.let { walker ->
            // follow position: place walking model in FRONT of where the player looks
            val followPos = currentPlayerLoc.clone()
            val offsetVec = player.eyeLocation.direction.normalize().multiply(1.2)
            followPos.add(offsetVec)

            // get y level under followPos (find first occluding block below within 10 blocks)
            val startY = followPos.y.toInt()
            var groundY = followPos.y
            for (y in startY downTo (startY - 10)) {
                val blockLoc = followPos.clone()
                blockLoc.y = y.toDouble()
                if (blockLoc.block.type.isOccluding) {
                    groundY = (y + 1).toDouble()
                    break
                }
            }
            followPos.y = groundY
            walker.teleport(followPos)

            // Decide orientation: prefer movement direction; if stationary and there's a distinct
            // target (getToLocation) inside player's FOV, orient to that target
            var desiredDir: Vector? = null

            if (movementDist > 0.02) {
                desiredDir = movementVec.normalize()
            } else {
                val targetDist = targetLocation.distance(currentPlayerLoc)
                if (targetDist > 1.0) {
                    // check if target is inside player's FOV
                    val eyeDir = player.eyeLocation.direction.normalize()
                    val toTarget = targetLocation.clone().subtract(player.eyeLocation).toVector().normalize()
                    val dot = eyeDir.dot(toTarget)
                    // cosine of ~45 degrees = 0.707 -> use 0.7 threshold
                    if (dot > 0.7) {
                        desiredDir = toTarget
                    }
                }
            }

            if (desiredDir != null) {
                // apply rotation using quaternion like flying model
                async {
                    val toVector = Vector3f(desiredDir.x.toFloat(), desiredDir.y.toFloat(), desiredDir.z.toFloat())
                    val fromVector = Vector3f(0f, -1f, 0f)
                    val rotationQuat = Quaternionf().rotationTo(fromVector, toVector)

                    sync {
                        try {
                            val entity = walker.entity
                            val oldTransform = entity.transformation
                            entity.transformation = Transformation(
                                oldTransform.translation,
                                rotationQuat,
                                oldTransform.scale,
                                oldTransform.rightRotation
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        // update last player location for next tick
        lastPlayerLocation = currentPlayerLoc

        pointAtTarget()
    }

    private fun pointAtTarget() {
        val entity = flyingModel?.entity ?: return
        if(targetingPlayer){
            resetOrbitLocation()
        }
        val playerLoc = orbitLocation
        val finalTargetLocation = orbitTarget
        val direction = finalTargetLocation.clone().subtract(playerLoc).toVector().normalize()
        val orbitLoc = playerLoc.clone().add(direction.multiply(radius)).add(0.0, 1.8, 0.0)

        flyingModel?.teleport(orbitLoc)

        async {
            val toTarget = finalTargetLocation.toVector().subtract(orbitLoc.toVector()).normalize()
            val fromVector = Vector3f(0f, -1f, 0f)
            val toVector = Vector3f(toTarget.x.toFloat(), toTarget.y.toFloat(), toTarget.z.toFloat())

            val rotationQuat = Quaternionf().rotationTo(fromVector, toVector)

            sync {
                val oldTransform = entity.transformation
                entity.transformation = Transformation(
                    oldTransform.translation,
                    rotationQuat,
                    oldTransform.scale,
                    oldTransform.rightRotation
                )
            }
        }
    }

}