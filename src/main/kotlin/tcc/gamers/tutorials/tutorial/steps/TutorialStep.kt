package tcc.gamers.tutorials.tutorial.steps

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.cinematic.TutorialCinematic
import tcc.gamers.tutorials.events.TutorialEvent
import tcc.gamers.tutorials.path.PathPointGenerator
import tcc.gamers.tutorials.tutorial.data.*
import tcc.gamers.tutorials.tutorial.listener.TutorialListener
import tcc.gamers.util.DynamicListener
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import tcc.gamers.tutorials.triggers.TriggerManager
import tcc.gamers.tutorials.tutorial.ui.display.ModelTutorialDisplay
import tcc.gamers.tutorials.tutorial.ui.display.TutorialDisplay
import tcc.gamers.util.later
import java.util.function.Supplier

abstract class TutorialStep<T: TutorialEvent>(
    private val eventClass: Class<T>,
    protected val player: Player,
    protected val plugin: TCCPlugin,
    val instructions: Title,
    val completion: Title?,
    val cinematicDto: CinematicDto? = null,
    val triggersDto: MutableList<TriggerDto>? = null,
    val getToLocationDto: GetToLocationDto? = null,
    val displayText: TitleDto? = null,
    val models: DisplayDto? = null
) {
    private lateinit var tutorialDisplay: TutorialDisplay
    private lateinit var pathPointGenerator: PathPointGenerator
    private val dynamicListener by lazy { dynamicListener() }
    private var showedCompletion = false
    private val listener = TutorialListener(player, eventClass,getToLocationDto)
    val gameMode = player.gameMode

    // trigger manager handles all trigger lifecycle
    private val triggerManager = TriggerManager(plugin, this)

    private fun targetSupplier(): Supplier<Location>{
        getToLocationDto?.let {
            it.location?.let { l ->
                return Supplier { l.build(player.world) }
            }
        }

        return Supplier { player.location }
    }

    private fun checkCompletion(){
        object : BukkitRunnable(){
            override fun run() {
                if(isCompleted()){
                    onCompletion()
                    this.cancel()
                    return
                }

            }
        }.runTaskTimer(plugin,0,0L)
    }

    private fun onCompletion() {
        if (dynamicListener.isPresent()) dynamicListener.stop()
        showCompletion()
        tutorialDisplay.cancel()
        stopPat()
        // end triggers using manager
        triggerManager.endAll(player)
        triggerManager.stop()
    }

    private fun stopPat() {
        if (::pathPointGenerator.isInitialized) {
            pathPointGenerator.cancel()
        }
    }

    private fun showCompletion() {
        if(completion != null){
            player.showTitle(completion)
            later({
                showedCompletion = true
            },20*2)
        }else{
            showedCompletion = true
        }
    }

    private fun dynamicListener(): DynamicListener {
        val dynamic = DynamicListener(plugin)
        dynamic.setListener(listener)
        return dynamic
    }


    private fun onStart(){
        tutorialDisplay = ModelTutorialDisplay(player,targetSupplier(), getCustomCinematicLocation(), displayText, models)
        startPath()
        startCinematic()
        // initialize and start triggers using manager
        triggerManager.initialize(triggersDto, player)
        triggerManager.startAll(player)
        triggerManager.startTicking()
        dynamicListener.start()
        player.showTitle(instructions)
        checkCompletion()
        tutorialDisplay.start()
    }

    private fun startPath() {
        getToLocationDto ?: return
        getToLocationDto.location ?: return
        val distance = getToLocationDto.distance ?: 1.5
        pathPointGenerator = PathPointGenerator(player.location, getToLocationDto.location!!.build(player.world),distance.toInt(), player)
        pathPointGenerator.start()
    }


    private fun getCustomCinematicLocation(): Location{
        var cinematicLocation: Location = targetSupplier().get()

        cinematicDto?.let {
            cinematicLocation = it.location?.build(player.world)!!
        }

        return cinematicLocation
    }

    private fun startCinematic(){
        if(cinematicDto != null){
            val cinematic = TutorialCinematic(player.location, getCustomCinematicLocation(), tutorialDisplay, cinematicDto)
            cinematic.setDuration(4)
            cinematic.play(player)
        }
    }

    fun finish(): Boolean{
        return isCompleted() && showedCompletion
    }

    protected fun isCompleted(): Boolean{
        return listener.completed
    }

    fun toDto():StepDto{
        val dto = StepDto()
        dto.event = eventClass.name.substringAfterLast('.')
        dto.models = models
        dto.cinematic = cinematicDto
        dto.displayText = displayText
        dto.instructions = TitleDto.fromTitle(instructions)
        completion?.let { dto.completion = TitleDto.fromTitle(completion) }
        getToLocationDto?.let {
            dto.getToLocation = getToLocationDto
        }
        dto.triggers = triggersDto

        return dto
    }

    fun start(){
        onStart()
    }

    fun cancel() {
        onCompletion()
    }

    // Public methods to access trigger manager when needed
    fun getTriggerManager(): TriggerManager = triggerManager

    fun hasTriggers(): Boolean = triggerManager.hasTriggers()

    fun getTriggerCount(): Int = triggerManager.getTriggerCount()
}
