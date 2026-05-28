package tcc.gamers.tutorials.tutorial.builder

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.events.TutorialEvent
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import tcc.gamers.tutorials.tutorial.Tutorial
import tcc.gamers.tutorials.tutorial.data.StepDto
import tcc.gamers.tutorials.tutorial.data.TutorialDto
import tcc.gamers.tutorials.tutorial.steps.StepBuilder
import tcc.gamers.tutorials.tutorial.steps.TutorialStep
import tcc.gamers.util.Builder

class TutorialBuilder(private val tutorialDto: TutorialDto, private val plugin: TCCPlugin, private val fileName: String): Builder<Tutorial, Player> {
    val name = tutorialDto.name
    val completionDto = tutorialDto.completion
    private var steps = mutableListOf<TutorialStep<out TutorialEvent>>()

    private fun addStep(vararg step: TutorialStep<out TutorialEvent>){
        steps.addAll(step)
    }

    fun buildAndAddStep(player: Player, clazz: Class<out TutorialEvent>, instructions: Title, completion: Title?, stepDto: StepDto){
        val step = StepBuilder(plugin,clazz).apply {
            this.instructions = instructions
            this.completion = completion
            this.cinematicDto = stepDto.cinematic
            this.triggers = stepDto.triggers
            this.getToLocation = stepDto.getToLocation
            this.displayText = stepDto.displayText
            this.models = stepDto.models
        }.build(player)

        addStep(step)
    }

    override fun build(arg: Player): Tutorial {
        val tutorial =  object : Tutorial(arg,plugin,tutorialDto,fileName){
            override fun registerSteps(): List<TutorialStep<out TutorialEvent>> {
                return this@TutorialBuilder.steps
            }

            override fun title(): String {
                return this@TutorialBuilder.name
            }

            override fun completion(): Title? {
                return this@TutorialBuilder.completionDto?.buildTitle()
            }

        }
        return tutorial
    }
}