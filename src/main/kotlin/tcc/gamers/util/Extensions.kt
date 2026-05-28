package tcc.gamers.util

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.builder.TutorialFactory
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Sound
import org.bukkit.command.CommandSender

private val plugin: JavaPlugin = JavaPlugin.getPlugin(TCCPlugin::class.java)

fun async(handler: () -> Unit) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, handler)
}


fun sync(handler: () -> Unit) {
    Bukkit.getScheduler().runTask(plugin, handler)
}


fun later(handler: () -> Unit, delay: Long) {
    Bukkit.getScheduler().runTaskLater(plugin, handler,delay)
}

fun timer(handler: () -> Unit, delay: Long, period: Long): BukkitTask {
    val runnable = Bukkit.getScheduler().runTaskTimer(plugin, handler,delay,period)
    return runnable
}

fun BukkitRunnable.timer(delay: Long,period: Long){
    this.runTaskTimer(plugin,delay,period)
}

fun timerRunnable(handler: () -> Unit): BukkitRunnable {
    val runnable = object : BukkitRunnable(){
        override fun run(){
            handler.invoke()
        }
    }
    return runnable
}

fun distance(p1: Location, p2: Location): Double{
    return p1.distance(p2)
}

fun Entity.hide(player: Player){
    player.hideEntity(TCCPlugin.instance,this)
}

fun Entity.hideAllExcept(player: Player){
    Bukkit.getOnlinePlayers().forEach {
        if(it != player) this.hide(it)
    }
}


fun Player.runTutorial(tutorialName: String, plugin: TCCPlugin) {
    try {
        val tutorialFactory = TutorialFactory(plugin)
        val tutorial = tutorialFactory.getById(tutorialName, this)
        tutorial ?: throw IllegalArgumentException("Tutorial with name $tutorialName not found")
        tutorial.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun BlockDisplay.rotateX(degrees: Double) {
    val old = this.transformation
    this.transformation = Transformation(
        old.translation,
        Quaternionf(Math.toRadians(degrees).toFloat(), 1f, 0f, 0f),
        old.scale,
        old.rightRotation
    )
}

fun BlockDisplay.rotateY(degrees: Double) {
    val old = this.transformation
    this.transformation = Transformation(
        old.translation,
        Quaternionf(Math.toRadians(degrees).toFloat(), 0f, 1f, 0f),
        old.scale,
        old.rightRotation
    )
}

fun BlockDisplay.rotateZ(degrees: Double) {
    val old = this.transformation
    this.transformation = Transformation(
        old.translation,
        Quaternionf(Math.toRadians(degrees).toFloat(), 0f, 0f, 1f),
        old.scale,
        old.rightRotation
    )
}

fun Color.toNamedTextColor(): NamedTextColor {
    val adventureTextColor: TextColor = TextColor.color(this.asRGB())

    return NamedTextColor.nearestTo(adventureTextColor)
}

private fun getTutorialCommonTag(): NamespacedKey{
    return NamespacedKey(plugin, "entity")
}

fun Entity.addTutorialTag(id: String){
    this.persistentDataContainer.set(getTutorialCommonTag(), PersistentDataType.STRING, id)
}

fun Entity.hasTutorialTag(): Boolean{
    return this.persistentDataContainer.has(getTutorialCommonTag())
}

/**
 * Send an Adventure Component to a CommandSender. Players will receive the Component directly,
 * console/other senders will receive a plain-text serialization.
 */
fun CommandSender.sendComponent(component: Component) {
    if (this is Player) {
        this.sendMessage(component)
    } else {
        val plain = PlainTextComponentSerializer.plainText().serialize(component)
        this.sendMessage(plain)
    }
}

/**
 * Send an action bar component to a player.
 */
fun Player.sendActionBarComponent(component: Component) {
    try {
        this.sendActionBar(component)
    } catch (_: NoSuchMethodError) {
        // fallback: send as chat if action bar not available
        this.sendMessage(component)
    }
}

fun resolveMinecraftSound(namespace: String, key: String): Sound? {
    val registry = RegistryAccess
        .registryAccess()
        .getRegistry(RegistryKey.SOUND_EVENT)

    return registry.get(NamespacedKey(namespace, key))
}


