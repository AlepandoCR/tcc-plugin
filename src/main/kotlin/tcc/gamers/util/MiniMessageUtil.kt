package tcc.gamers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

object MiniMessageUtil {
    private val MINI_MESSAGE = MiniMessage.miniMessage()

    /**
     * Parses a text using MiniMessage. If parsing fails, returns a plain text [Component] with the same content.
     * If the input is null, returns an empty component.
     * @param text source string, possibly containing MiniMessage tags
     * @return parsed component or plain text component on error, or empty component if null
     */
    fun parseOrPlain(text: String?): Component {
        if (text == null) {
            return Component.empty()
        }
        return try {
            MINI_MESSAGE.deserialize(text)
        } catch (_: Throwable) {
            Component.text(text)
        }
    }

    /**
     * Applies placeholders of the form %key% using a simple Map replacement, then parses with MiniMessage.
     * Falls back to plain text when MiniMessage parsing fails.
     * If the template is null, returns an empty component.
     * @param template template string that may contain placeholders like %player%
     * @param placeholders map of placeholder keys to values; may be null or empty
     * @return parsed component or plain text component on error, or empty component if template is null
     */
    fun parseOrPlain(template: String?, placeholders: MutableMap<String?, String?>?): Component {
        if (template == null) {
            return Component.empty()
        }
        var resolved: String? = template
        if (!placeholders.isNullOrEmpty()) {
            for (entry in placeholders.entries) {
                val keyToken: String = entry.key!!
                val valueText: String = (if (entry.value == null) "" else entry.value)!!
                resolved = resolved!!.replace(keyToken, valueText)
            }
        }
        return parseOrPlain(resolved)
    }
}