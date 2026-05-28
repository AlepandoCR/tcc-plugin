package tcc.gamers.tutorials.tutorial.ui.display

import tcc.gamers.tutorials.entities.HiddenPlayerManager
import tcc.gamers.tutorials.model.Model
import tcc.gamers.tutorials.tutorial.data.DisplayDto
import tcc.gamers.tutorials.tutorial.data.TitleDto
import tcc.gamers.util.addTutorialTag
import tcc.gamers.util.hideAllExcept
import tcc.gamers.util.timer
import tcc.gamers.util.timerRunnable
import tcc.gamers.util.async
import tcc.gamers.util.sync
import org.joml.Quaternionf
import org.joml.Vector3f
import org.bukkit.util.Transformation
import org.bukkit.Location
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.scheduler.BukkitRunnable
import java.util.function.Supplier

typealias DisplayInterface = tcc.gamers.tutorials.tutorial.ui.display.Display

abstract class TutorialDisplay(protected val player: Player, protected val target: Supplier<Location>, private val displayText: TitleDto? = null, models: DisplayDto? = null):
    DisplayInterface {
    private val world = player.world
    val location = player.location
    var model: Model<BlockDisplay>? = null
    var flyingModel: Model<BlockDisplay>? = null
    var walkingModel: Model<BlockDisplay>? = null
    val entity: BlockDisplay?
        get() = model?.entity
    var modelAnimation: String? = null
    var flyingModelAnimation: String? = null
    var walkingModelAnimation: String? = null
    lateinit var textDisplay: TextDisplay
    private val loop: BukkitRunnable

    init {
        val modelData = models?.pointModel
        val flyingModelData = models?.flyingModel
        val walkingModelData = models?.walkingModel

        modelAnimation = modelData?.animation
        flyingModelAnimation = flyingModelData?.animation
        walkingModelAnimation = walkingModelData?.animation

        modelData?.let { md ->
            val name = md.name
            model = Model.spawn(name, location, BlockDisplay::class.java)
            model?.entity?.addTutorialTag("tutorialDisplay")
            // apply optional scale from config
            val s = md.scale
            if (s != null) model?.scale(s)
        }

        flyingModelData?.let { md ->
            val name = md.name
            flyingModel = Model.spawn(name, location, BlockDisplay::class.java)
            flyingModel?.entity?.addTutorialTag("tutorialDisplay")
            // apply optional scale from config
            val s = md.scale
            if (s != null) flyingModel?.scale(s)
        }

        walkingModelData?.let { md ->
            val name = md.name
            walkingModel = Model.spawn(name, location, BlockDisplay::class.java)
            walkingModel?.entity?.addTutorialTag("tutorialDisplay")
            // apply optional scale from config
            val s = md.scale
            if (s != null) walkingModel?.scale(s)
        }

        loop = timerRunnable {
            tick()
            tpText()
            orientPointModelToPlayer()
        }


    }

    private fun text(){
        displayText?.let {
            val title = it.buildTitle()
            textDisplay = world.spawn(target.get(),TextDisplay::class.java)
            textDisplay.addTutorialTag("tutorialDisplay")   
            val components = title.title().appendNewline().append(title.subtitle())
            textDisplay.billboard = Display.Billboard.CENTER
            textDisplay.isShadowed = true
            textDisplay.text(components)
            textDisplay.hideAllExcept(player)
            HiddenPlayerManager.add(textDisplay)
        }
    }

    private fun tpText(){
        if(textDisplayInitialized() && model != null){
           textDisplay.teleport( model?.entity?.location?.add(0.0,1.0,0.0) ?: return)
        }
    }

    // Orient the pointing model to look at the player from its spawn/current position
    private fun orientPointModelToPlayer() {
        val m = model ?: return
        try {
            val entity = m.entity
            async {
                try {
                    val toTarget = player.location.toVector().subtract(entity.location.toVector()).normalize()
                    val fromVector = Vector3f(0f, -1f, 0f)
                    val toVector = Vector3f(toTarget.x.toFloat(), toTarget.y.toFloat(), toTarget.z.toFloat())
                    val rotationQuat = Quaternionf().rotationTo(fromVector, toVector)

                    sync {
                        try {
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
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    fun textDisplayInitialized(): Boolean{
        return ::textDisplay.isInitialized
    }

    abstract fun tick()

    override fun start(){
        model?.let { HiddenPlayerManager.add(it.entity) }
        flyingModel?.let { HiddenPlayerManager.add(it.entity) }
        walkingModel?.let { HiddenPlayerManager.add(it.entity) }
        loop.timer(0,0)
        text()
        model?.hideToAllExcept(player)
        flyingModel?.hideToAllExcept(player)
        walkingModel?.hideToAllExcept(player)
        flyingModel?.animate(flyingModelAnimation ?: "idle")
        model?.animate(modelAnimation ?:"idle")
        walkingModel?.animate(walkingModelAnimation ?: "walk")
    }

    override fun cancel() {
        loop.cancel()
        model?.despawn()
        flyingModel?.despawn()
        walkingModel?.despawn()
        if(textDisplayInitialized())textDisplay.remove()
    }
}