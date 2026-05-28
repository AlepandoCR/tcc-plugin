package tcc.gamers.util.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import java.util.Locale.getDefault

/**
 * The internal registry and PAPI expansion for tcc.
 * Registers custom variables to be usable as %spartan_<identifier>% in other plugins.
</identifier> */
class TCCExpansion : PlaceholderExpansion() {
    private val registry: MutableMap<String?, PlaceholderVar<*>?> = HashMap()

    /**
     * Registers a typed variable into the PAPI expansion.
     *
     * @param variable The placeholder variable to register.
     */
    fun registerVariable(variable: PlaceholderVar<*>) {
        registry[variable.identifier.lowercase(getDefault())] = variable
    }

    override fun getIdentifier(): String{
        return "spartan"
    }

    override fun getAuthor(): String{
        return "Alepando"
    }


    override fun  getVersion(): String{
        return "1.0.0"
    }

    override fun persist(): Boolean {
        return true // Keeps the expansion loaded across PAPI reloads
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val variable: PlaceholderVar<*>? = registry[params.lowercase(getDefault())]

        if (variable != null) {
            return variable.asString
        }

        return null // Return null if the placeholder is not handled by us
    }
}