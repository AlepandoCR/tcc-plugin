package tcc.gamers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import java.time.Duration

class TitleBuilder {
    private var title: Component = Component.empty()
    private var subtitle: Component = Component.empty()
    private var fadeIn: Duration = Duration.ofMillis(500)
    private var stay: Duration = Duration.ofSeconds(3)
    private var fadeOut: Duration = Duration.ofMillis(500)

    fun title(text: String, color: TextColor? = null, bold: Boolean = false, italic: Boolean = false): TitleBuilder {
        title = buildText(text, color, bold)
        return this
    }

    fun subtitle(text: String, color: TextColor? = null, italic: Boolean = false, bold: Boolean = false): TitleBuilder {
        subtitle = buildText(text, color, italic = italic)
        return this
    }

    fun fadeIn(ticks: Int): TitleBuilder {
        fadeIn = Duration.ofMillis(ticks * 50L)
        return this
    }

    fun stay(ticks: Int): TitleBuilder {
        stay = Duration.ofMillis(ticks * 50L)
        return this
    }

    fun fadeOut(ticks: Int): TitleBuilder {
        fadeOut = Duration.ofMillis(ticks * 50L)
        return this
    }

    fun build(): Title {
        val times = Title.Times.times(fadeIn, stay, fadeOut)
        return Title.title(title, subtitle, times)
    }

    companion object {
        fun component(text: String, color: TextColor? = null, bold: Boolean = false, italic: Boolean = false): Component {
            return buildText(text, color, bold, italic)
        }

        private fun buildText(text: String, color: TextColor? = null, bold: Boolean = false, italic: Boolean = false): TextComponent {
            var comp = Component.text(text)
            if (color != null) comp = comp.color(color)
            if (bold) comp = comp.decorate(TextDecoration.BOLD)
            if (italic) comp = comp.decorate(TextDecoration.ITALIC)
            return comp
        }

        fun color(name: String): TextColor? = NamedTextColor.NAMES.value(name.lowercase())
        fun rgb(r: Int, g: Int, b: Int): TextColor = TextColor.color(r, g, b)
    }
}
