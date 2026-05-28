package tcc.gamers.tutorials.tutorial.manager

import org.bukkit.Location
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import tcc.gamers.tutorials.tutorial.data.PathDto
import tcc.gamers.tutorials.tutorial.data.PathLocationDto
import tcc.gamers.util.Path
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

/**
 * Manager to persist and load Paths from YAML files.
 * Each path is identified by a unique ID.
 */
class PathManager(private val pathsDirectory: File) {

    private val yaml: Yaml

    init {
        if (!pathsDirectory.exists()) {
            pathsDirectory.mkdirs()
        }

        // Configure DumperOptions to not include class tags
        val dumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        dumperOptions.isPrettyFlow = true

        // Configure LoaderOptions to be lenient
        val loaderOptions = LoaderOptions()

        // Create constructor
        val constructor = Constructor(PathDto::class.java, loaderOptions)

        // Create representer and register our custom classes as plain maps (no tags)
        val representer = Representer(dumperOptions)
        representer.addClassTag(PathDto::class.java, Tag.MAP)
        representer.addClassTag(PathLocationDto::class.java, Tag.MAP)

        // Create Yaml instance
        yaml = Yaml(constructor, representer, dumperOptions)
    }

    /**
     * Saves a Path to a YAML file.
     * The file is named as: <id>.yml
     */
    fun savePath(id: String, path: Path): Boolean {
        return try {
            val pathDto = PathDto.fromPath(id, path)
            val file = File(pathsDirectory, "$id.yml")

            FileWriter(file).use { writer ->
                yaml.dump(pathDto, writer)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads a Path from a YAML file.
     * Returns null if the file does not exist or there is a deserialization error.
     */
    fun loadPath(id: String, defaultLocation: Location? = null): Path? {
        return try {
            val file = File(pathsDirectory, "$id.yml")
            if (!file.exists()) return null

            FileInputStream(file).use { inputStream ->
                val pathDto = yaml.load<PathDto>(inputStream)
                pathDto?.toPath(defaultLocation)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lists all IDs of saved paths.
     */
    fun listPathIds(): List<String> {
        return pathsDirectory.listFiles()?.mapNotNull { file ->
            if (file.extension == "yml") {
                file.nameWithoutExtension
            } else null
        } ?: emptyList()
    }

    /**
     * Deletes a saved path.
     */
    @Suppress("unused")
    fun deletePath(id: String): Boolean {
        return try {
            val file = File(pathsDirectory, "$id.yml")
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if a path exists.
     */
    fun pathExists(id: String): Boolean {
        return File(pathsDirectory, "$id.yml").exists()
    }

    fun getPaths(): Collection<Path> {
        val pathsUnchecked = listPathIds().map { loadPath(it) }

        return pathsUnchecked.filterNotNull()
    }
}


