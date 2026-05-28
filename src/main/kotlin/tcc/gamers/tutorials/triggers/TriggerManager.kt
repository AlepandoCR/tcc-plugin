package tcc.gamers.tutorials.triggers

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.tutorial.data.TriggerDto
import tcc.gamers.tutorials.tutorial.steps.TutorialStep

/**
 * Manages the lifecycle of triggers within a tutorial step.
 * Handles initialization, starting, ticking, and ending of all triggers.
 */
class TriggerManager(private val plugin: TCCPlugin, val step: TutorialStep<*>) {
    private val triggers = mutableListOf<TutorialTrigger>()
    private var tickTask: BukkitRunnable? = null
    private var currentPlayer: Player? = null

    fun initialize(triggersDto: MutableList<TriggerDto>?, player: Player) {
        triggers.clear()
        currentPlayer = player
        triggersDto?.forEach { dto ->
            try {
                val trigger = createTriggerFromDto(dto, player)
                trigger?.let { triggers.add(it) }
            } catch (ex: Exception) {
                plugin.logger.warning("Failed to initialize trigger: ${ex.message}")
            }
        }
    }

    private fun createTriggerFromDto(triggerDto: TriggerDto, player: Player): TutorialTrigger? {
        // Detect sound trigger
        val isSound = triggerDto.type?.equals("sound", ignoreCase = true) == true
                || (triggerDto.namespace != null && triggerDto.key != null)
                || (triggerDto.id != null && triggerDto.id!!.contains(":"))

        if (isSound) {
            val (ns, key) = when {
                triggerDto.namespace != null && triggerDto.key != null -> Pair(
                    triggerDto.namespace!!,
                    triggerDto.key!!
                )
                triggerDto.id != null && triggerDto.id!!.contains(":") -> triggerDto.id!!
                    .split(":", limit = 2)
                    .let { Pair(it[0], it[1]) }
                else -> Pair("minecraft", triggerDto.id ?: "")
            }
            val namespaced = NamespacedKey(ns, key)
            val soundTrigger = SoundTrigger(namespaced)
            soundTrigger.id = triggerDto.id?.let { if (it.startsWith("sound:")) it else "sound:${it}" } ?: "sound:${ns}:${key}"
            return soundTrigger
        }

        // Create custom trigger
        return triggerDto.id?.let { id ->
            val custom = TriggerRegistry.create(id, player)
            custom?.let {
                try { it.id = id } catch (_: Throwable) {}
                return@let it
            }
        }
    }

    fun startAll(player: Player) {
        triggers.forEach { try { it.onStart(player) } catch (_: Exception) {} }
    }

    fun startTicking() {
        if (triggers.isEmpty()) return
        val task = object : BukkitRunnable() {
            override fun run() {
                currentPlayer?.let { player ->
                    triggers.forEach { try { it.onTick(player) } catch (_: Exception) {} }

                    if(canCompleteStep()){ // if any completed step trigger is ready, complete the step and end this
                        step.finish()
                        endAll(currentPlayer!!)
                        stop()

                    }
                }
                if(currentPlayer == null || !currentPlayer!!.isOnline || !currentPlayer!!.isValid){ // if player is null or offline, end triggers and stop ticking
                    stop() // force stop if player is null
                }
            }
        }
        task.runTaskTimer(plugin, 0L, 1L)
        tickTask = task
    }

    fun endAll(player: Player) {
        triggers.forEach { try { it.onEnd(player) } catch (_: Exception) {} }
    }

    fun stop() {
        try { tickTask?.cancel() } catch (_: Exception) {}
        tickTask = null
    }

    fun clear() {
        stop()
        triggers.clear()
        currentPlayer = null
    }

    fun getTriggers(): List<TutorialTrigger> = triggers.toList()

    fun getTickTask(): BukkitRunnable? = tickTask

    fun getTriggerCount(): Int = triggers.size

    fun hasTriggers(): Boolean = triggers.isNotEmpty()

    fun canCompleteStep(): Boolean = triggers
        .stream()
        .filter { it is TutorialStepTrigger }
        .map { it as TutorialStepTrigger }
        .filter { it.canCompleteStep() }
        .toList()
        .isNotEmpty()
}





