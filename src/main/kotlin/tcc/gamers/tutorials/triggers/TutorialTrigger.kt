package tcc.gamers.tutorials.triggers

import org.bukkit.entity.Player

/**
 * Represents a trigger that lives inside a tutorial step.
 * Implementations should be safe to call frequently from the server tick.
 */
interface TutorialTrigger {
    // optional id assigned to the trigger (populated from DTO or registry key)
    var id: String?

    fun onStart(player: Player)
    fun onTick(player: Player)
    fun onEnd(player: Player)
}


