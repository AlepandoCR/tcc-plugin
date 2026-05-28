package tcc.gamers.tutorials.path

import tcc.gamers.tutorials.tutorial.ui.display.Display
import org.bukkit.Location
import org.bukkit.entity.Player

abstract class PathDisplay(protected val player: Player, var location: Location, val spacing: Int) : Display