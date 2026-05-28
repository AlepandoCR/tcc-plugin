package tcc.gamers.tutorials.events.types

import tcc.gamers.tutorials.events.TutorialPlayerEvent
import tcc.gamers.tutorials.tutorial.Tutorial
import org.bukkit.entity.Player

class PlayerCompleteTutorialEvent(player: Player, val tutorial: Tutorial) : TutorialPlayerEvent(player)