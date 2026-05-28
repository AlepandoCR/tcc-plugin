package tcc.gamers.util.store

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.util.logging.Level

class YamlStore(private val plugin: JavaPlugin, private val file: File) {

    private var config: FileConfiguration? = null

    init {
        this.load()
    }

    private fun load() {
        file.parentFile?.takeIf { !it.exists() }?.mkdirs()

        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                plugin.logger.log(Level.SEVERE, "Failed to create storage file: ${file.name}", e)
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file)
    }

    /** Writes or overwrites a value at the given dot-notation path. */
    fun write(path: String, value: Any?) {
        config!!.set(path, value)
    }

    /** Reads a value and casts it automatically. Returns null if not found. */
    @Suppress("UNCHECKED_CAST")
    fun <T> read(path: String): T? = config!!.get(path) as T?


    /** Reads a value with a fallback default. */
    @Suppress("UNCHECKED_CAST")
    fun <T> readOrDefault(path: String, defaultValue: T): T =
        (config!!.get(path) as T?) ?: defaultValue

    /** Returns true if the path exists and is non-null. */
    fun has(path: String): Boolean = config!!.contains(path)

    /** Sets the path to null, effectively removing it. */
    fun delete(path: String) {
        config!!.set(path, null)
    }

    /** Commits the memory tree to disk synchronously. */
    fun save() {
        try {
            config!!.save(file)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Failed to save storage file: ${file.name}", e)
        }
    }

    /** Commits the memory tree to disk asynchronously. */
    fun saveAsync() {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { save() })
    }

    /** Reloads from disk — unsaved in-memory changes are lost. */
    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)
    }

    /** Exposes the underlying file for external use (e.g., passing to another YamlStore). */
    fun getFile(): File = file
}