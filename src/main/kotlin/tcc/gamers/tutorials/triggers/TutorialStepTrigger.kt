package tcc.gamers.tutorials.triggers

interface TutorialStepTrigger: TutorialTrigger {
    fun canCompleteStep(): Boolean
}