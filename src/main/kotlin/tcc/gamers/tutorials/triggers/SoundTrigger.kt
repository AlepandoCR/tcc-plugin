package tcc.gamers.tutorials.triggers

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import tcc.gamers.util.resolveMinecraftSound

class SoundTrigger(private val key: NamespacedKey?) : TutorialTrigger {
    override var id: String? = null

    override fun onStart(player: Player) {
        try {
            key?.let {
                val sound = resolveMinecraftSound(it.namespace, it.key)
                sound?.let { s -> player.playSound(player.location, s, 1f, 1f) }
            }
        } catch (_: NoSuchMethodError) {
            // older servers: ignore
        }
    }

    override fun onTick(player: Player) {}

    override fun onEnd(player: Player) {}
}



