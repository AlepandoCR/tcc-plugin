package tcc.gamers.tutorials.cinematic

import org.bukkit.entity.Player

interface Cinematic {
    fun play(player: Player)

    fun stop(player: Player)

    fun isPlaying(): Boolean
}