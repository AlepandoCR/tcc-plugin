package tcc.gamers.tutorials.tutorial.steps

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.events.TutorialEvent
import tcc.gamers.tutorials.tutorial.data.*
import tcc.gamers.util.Builder
import tcc.gamers.util.TitleBuilder
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

class StepBuilder<T: TutorialEvent> (private val plugin: TCCPlugin, private val eventClass:Class<T>):
    Builder<TutorialStep<T>, Player> {
    var instructions: Title = TitleBuilder().title("N/D").build()
    var completion: Title? = null
    var cinematicDto: CinematicDto? = null
    var triggers: MutableList<tcc.gamers.tutorials.tutorial.data.TriggerDto>? = null
    var getToLocation: GetToLocationDto? = null
    var displayText: TitleDto? = null
    var models: DisplayDto? = null

    override fun build(arg: Player): TutorialStep<T> {
        return object : TutorialStep<T>(eventClass,arg,plugin,instructions,completion, cinematicDto, triggers, getToLocation,displayText,models){}
    }
}