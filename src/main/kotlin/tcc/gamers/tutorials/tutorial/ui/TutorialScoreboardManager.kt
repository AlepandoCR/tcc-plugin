package tcc.gamers.tutorials.tutorial.ui

import tcc.gamers.TCCPlugin
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import tcc.gamers.tutorials.tutorial.Tutorial
import tcc.gamers.tutorials.tutorial.steps.queue.StepQueue

class TutorialScoreboardManager(
    private val plugin: TCCPlugin,
    private val queue: StepQueue,
    private val player: Player,
    private val tutorial: Tutorial
) {
    private var scoreboard: Scoreboard
    private var objective: Objective
    private var lastTitle: String? = null

    private val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
        update()
    }, 0L, 10L)

    init {
        scoreboard = Bukkit.getScoreboardManager().newScoreboard
        objective = scoreboard.registerNewObjective(player.name, "dummy", "§d${tutorial.title()}")
        objective.displaySlot = DisplaySlot.SIDEBAR
        player.scoreboard = scoreboard
    }

    private fun update() {
        val currentStep = queue.steps.firstOrNull() ?: return
        val instructions = currentStep.instructions
        val title = (instructions.title() as TextComponent).content()
        val subtitle = (instructions.subtitle() as TextComponent).content()
        val index = queue.currentStepIndex
        val total = queue.totalSteps

        updateTitle()

        val entries = scoreboard.entries
        entries.forEach { scoreboard.resetScores(it) }

        val wrappedLines = if (subtitle.isNotBlank()) wrapText(subtitle, 25) else emptyList()
        var score = 2 + wrappedLines.size

        objective.getScore("§7Step $index/$total").score = score--

        if (title.isNotBlank()) {
            objective.getScore("§a$title").score = score--
        }

        for (line in wrappedLines) {
            objective.getScore("§f$line").score = score--
        }
    }

    private fun updateTitle() {
        val newTitle = "§d${tutorial.title()}"
        if (newTitle != lastTitle) {
            objective.displayName = newTitle
            lastTitle = newTitle
        }
    }

    fun stop() {
        task.cancel()
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    private fun wrapText(text: String, maxLineLength: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 > maxLineLength) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }


}
