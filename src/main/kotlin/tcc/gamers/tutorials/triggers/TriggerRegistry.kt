package tcc.gamers.tutorials.triggers

import org.bukkit.entity.Player

/**
 * Registry for custom triggers implemented in code. Register triggers at plugin startup using
 * TriggerRegistry.register("my_trigger", ::MyTriggerFactory)
 */
object TriggerRegistry {
    private val registry = mutableMapOf<String, (Player) -> TutorialTrigger>()

    fun register(id: String, factory: (Player) -> TutorialTrigger) {
        registry[id] = factory
    }

    fun unregister(id: String) {
        registry.remove(id)
    }

    fun create(id: String, player: Player): TutorialTrigger? {
        return registry[id]?.invoke(player)
    }
}

