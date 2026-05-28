package tcc.gamers.tutorials.events.types

import tcc.gamers.tutorials.events.TutorialPlayerEvent
import org.bukkit.Location
import org.bukkit.entity.Player

class PlayerGetToLocationEvent(
    player: Player,
    val destination: Location,
): TutorialPlayerEvent(player)