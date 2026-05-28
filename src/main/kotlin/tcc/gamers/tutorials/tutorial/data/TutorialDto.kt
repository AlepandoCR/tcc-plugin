package tcc.gamers.tutorials.tutorial.data

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.Tutorial
import tcc.gamers.tutorials.tutorial.builder.TutorialFactory
import tcc.gamers.util.TitleBuilder
import tcc.gamers.util.sync
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

class TutorialDto {
    lateinit var id: String
    lateinit var name: String
    lateinit var steps: MutableList<StepDto>
    var world: String = "N/D"
    var completion: TitleDto? = null
    var islandTutorial = false
    var active: Boolean = false

    private fun build(player: Player): Tutorial {
        val factory = TutorialFactory(TCCPlugin.instance)
        val originalTutorial = factory.getById(id, player)
        originalTutorial ?: throw NoSuchElementException("No matching tutorial id found")

        return originalTutorial
    }

    fun buildAndPlay(player: Player){
        sync { build(player).start() }
    }

    fun checkActivity() {
        active = steps.isNotEmpty()
    }

}

class StepDto {
    lateinit var event: String
    lateinit var instructions: TitleDto
    var completion: TitleDto? = null
    var cinematic: CinematicDto? = null
    var triggers: MutableList<TriggerDto>? = null
    var getToLocation: GetToLocationDto? = null
    var displayText: TitleDto? = null
    var models: DisplayDto? = null
}

class TriggerDto {
    // type can be "sound" or "custom". If null or "custom" it's treated as custom trigger id.
    var type: String? = null
    // for custom triggers: id (key used to lookup registered custom triggers)
    var id: String? = null
    // for sound triggers: either provide namespace/key separately or id as "namespace:key"
    var namespace: String? = null
    var key: String? = null
}

class TitleDto {
    lateinit var title: String
    var subtitle: String? = null
    var titleColor: String? = null
    var subtitleColor: String? = null
    var boldTitle: Boolean? = false
    var italicTitle: Boolean? = false
    var boldSubtitle: Boolean? = false
    var italicSubtitle: Boolean? = false
    var fadeIn: Int? = null
    var stay: Int? = null
    var fadeOut: Int? = null

    fun buildTitle(): Title {
        val titleBuilder = TitleBuilder()
            .title(
                title,
                titleColor?.let { TitleBuilder.color(it) },
                boldTitle ?: false,
                italicTitle ?: false
            )

        subtitle?.let {
            titleBuilder.subtitle(
                it,
                subtitleColor?.let { color -> TitleBuilder.color(color) },
                italicSubtitle ?: false,
                boldSubtitle ?: false
            )
        }

        fadeIn?.let { titleBuilder.fadeIn(it) }
        stay?.let { titleBuilder.stay(it) }
        fadeOut?.let { titleBuilder.fadeOut(it) }

        return titleBuilder.build()
    }

    companion object {

        fun fromTitle(title: Title): TitleDto {
            val dto = TitleDto()

            val titleComponent = title.title()
            if (titleComponent != Component.empty()) {
                dto.title = (titleComponent as? net.kyori.adventure.text.TextComponent)?.content() ?: ""
                dto.titleColor = titleComponent.style().color()?.let { NamedTextColor.nearestTo(it).toString() }
                dto.boldTitle = titleComponent.style().hasDecoration(TextDecoration.BOLD)
                dto.italicTitle = titleComponent.style().hasDecoration(TextDecoration.ITALIC)
            } else {
                dto.title = ""
            }


            val subtitleComponent = title.subtitle()
            if (subtitleComponent != Component.empty()) {
                dto.subtitle = (subtitleComponent as? net.kyori.adventure.text.TextComponent)?.content()
                dto.subtitleColor = subtitleComponent.style().color()?.let { NamedTextColor.nearestTo(it).toString() }
                dto.boldSubtitle = subtitleComponent.style().hasDecoration(TextDecoration.BOLD)
                dto.italicSubtitle = subtitleComponent.style().hasDecoration(TextDecoration.ITALIC)
            }

            title.times()?.let { times ->
                dto.fadeIn = times.fadeIn().toMillis().toInt() / 50
                dto.stay = times.stay().toMillis().toInt() / 50
                dto.fadeOut = times.fadeOut().toMillis().toInt() / 50
            }

            return dto
        }
    }
}

class CinematicDto {
    var location: LocationDto? = null
    // Optional final look-at location: where the camera should be looking at the end
    var lookAt: LocationDto? = null
    var speed: Double? = null // speed of the cinematic camera
    var distance: Double? = null // distance to the target to show
    var duration: Int? = null
}

class DisplayDto {
    var pointModel: ModelDto? = null
    var flyingModel: ModelDto? = null
    var walkingModel: ModelDto? = null
}

class ModelDto{
    lateinit var name: String
    var animation: String? = null
    var scale: Double? = null
}

class GetToLocationDto {
    var location: LocationDto? = null
    var distance: Double? = null

    fun isNear(player: Player) : Boolean{
        val world = player.world
        location ?: throw NoSuchElementException("location not defined in - step - getToLocation - location")
        distance ?: throw NoSuchElementException("distance not defined in - step - getToLocation - location")
        val distanceBetween = location!!.build(world).distance(player.location)
        return distanceBetween < distance!!
    }

    fun isNear(locationP: Location) : Boolean{
        val world = locationP.world
        location ?: throw NoSuchElementException("location not defined in - step - getToLocation - location")
        distance ?: throw NoSuchElementException("distance not defined in - step - getToLocation - location")
        return location!!.build(world).distance(locationP) < distance!!
    }
}

class LocationDto{
    var x: Double? = null
    var y: Double? = null  // coords to get to
    var z: Double? = null

    fun build(world: World) : Location {
        x ?: throw NoSuchElementException("X not defined in - step - cinematic - location")
        y ?: throw NoSuchElementException("Y not defined in - step - cinematic - location")
        z ?: throw NoSuchElementException("Z not defined in - step - cinematic - location")
       return Location(world, x!!,y!!,z!!)
    }
}
