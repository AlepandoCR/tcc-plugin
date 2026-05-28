package tcc.gamers.tutorials.tutorial.steps.queue

import tcc.gamers.TCCPlugin
import tcc.gamers.tutorials.events.TutorialEvent
import org.bukkit.scheduler.BukkitRunnable
import tcc.gamers.tutorials.tutorial.Tutorial
import tcc.gamers.tutorials.tutorial.data.StepDto
import tcc.gamers.tutorials.tutorial.steps.TutorialStep

class StepQueue(private val plugin: TCCPlugin) {
    var steps = mutableListOf<TutorialStep<out TutorialEvent>>()
    var totalSteps = 0
    var currentStepIndex = 1
    private lateinit var currentStep: TutorialStep<out TutorialEvent>
    private var setCancelled = false

    fun cancel(){
        if(::currentStep.isInitialized) currentStep.cancel()
        setCancelled = true
    }

    fun addStep(step: TutorialStep<out TutorialEvent>){
        steps.add(step)
        totalSteps++
    }

    fun removeStep(step: TutorialStep<out TutorialEvent>){
        steps.remove(step)
    }

    fun dtoList(): MutableList<StepDto>{
        val list = mutableListOf<StepDto>()
        steps.forEach {
            list.add(it.toDto())
        }
        return list
    }

    fun cycleSteps(tutorial: Tutorial) {
        if (steps.isEmpty()) return

        currentStep = steps.first()
        currentStep.start()

        object : BukkitRunnable() {
            override fun run() {
                if (currentStep.finish()) {
                    removeStep(currentStep)
                    if (steps.isEmpty() || setCancelled) {
                        tutorial.complete(setCancelled)
                        cancel()
                        return
                    }
                    currentStep = steps.first()
                    currentStepIndex++
                    currentStep.start()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}