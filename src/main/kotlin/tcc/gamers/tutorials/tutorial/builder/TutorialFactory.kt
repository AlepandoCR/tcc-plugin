package tcc.gamers.tutorials.tutorial.builder

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.events.TutorialEvent
import tcc.gamers.tutorials.events.types.PlayerGetToLocationEvent
import org.bukkit.entity.Player
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import tcc.gamers.tutorials.tutorial.Tutorial
import tcc.gamers.tutorials.tutorial.data.StepDto
import tcc.gamers.tutorials.tutorial.data.TutorialDto
import java.io.File
import java.io.FileInputStream

class TutorialFactory(private val plugin: TCCPlugin) {
    private val tutorialsDir = File(plugin.dataFolder, "tutorials/")

    init {
        if(!tutorialsDir.exists()){
            tutorialsDir.mkdir()
        }
    }

    @Suppress("unused")
    fun load(tutorialName: String, player: Player): Tutorial {
        val tutorialFile = File(plugin.dataFolder, "tutorials/$tutorialName.yml")
        if (!tutorialFile.exists()) {
            throw NoSuchElementException("No tutorial named $tutorialName")
        }

        val yaml = Yaml(Constructor(TutorialDto::class.java, LoaderOptions()))
        val tutorialDto: TutorialDto = yaml.load(FileInputStream(tutorialFile))

        val tutorialBuilder = TutorialBuilder(tutorialDto, plugin, tutorialName)

        addStepsToBuilder(tutorialDto, tutorialBuilder, player)

        return tutorialBuilder.build(player)
    }

    @Suppress("UNCHECKED_CAST")
    private fun findEventClass(stepDto: StepDto): Class<out TutorialEvent> {
        return if (stepDto.getToLocation != null) {
            PlayerGetToLocationEvent::class.java
        } else {
            val basePackage = PlayerGetToLocationEvent::class.java.packageName
            try {
                Class.forName("$basePackage.${stepDto.event}") as Class<out TutorialEvent>
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Event class not found: ${stepDto.event}. Looked in package: $basePackage", e)
            }
        }
    }

    private fun addStepsToBuilder(
        tutorialDto: TutorialDto,
        tutorialBuilder: TutorialBuilder,
        player: Player
    ) {
        tutorialDto.steps.forEach { stepDto ->
            val eventClass = findEventClass(stepDto)

            val instructions = stepDto.instructions.buildTitle()
            val completion = stepDto.completion?.buildTitle()
            tutorialBuilder.buildAndAddStep(player, eventClass, instructions, completion, stepDto)
        }
    }

    private fun loadTutorialDto(tutorialName: String): TutorialDto {
        val tutorialFile = File(plugin.dataFolder, "tutorials/$tutorialName.yml")

        if (!tutorialFile.exists()) {
            throw java.io.FileNotFoundException("File not found ${tutorialFile.absolutePath}")
        }

        val yaml = Yaml(Constructor(TutorialDto::class.java, LoaderOptions()))
        return yaml.load(FileInputStream(tutorialFile))
    }

    fun getById(id: String, player: Player):Tutorial?{
        var filtered: TutorialDto? = null
        var fileName: String? = null

        for (tutorialFile in tutorialsDir.listFiles() ?: emptyArray()) {
            val tutorial = loadTutorialDto(tutorialFile.name.substringBeforeLast('.'))
            if(tutorial.id == id){
                filtered = tutorial
                fileName = tutorialFile.name
                break
            }
        }

        filtered ?: return null
        fileName ?: return null

        val builder = TutorialBuilder(filtered,plugin,fileName)
        addStepsToBuilder(filtered,builder,player)
        val tutorial = builder.build(player)

        return tutorial
    }
}
