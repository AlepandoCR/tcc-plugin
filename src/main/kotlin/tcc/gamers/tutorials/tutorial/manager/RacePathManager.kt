package tcc.gamers.tutorials.tutorial.manager

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import tcc.gamers.tutorials.tutorial.data.PathDto
import tcc.gamers.tutorials.tutorial.data.PathLocationDto
import tcc.gamers.util.path.RacePath
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager to persist, load, and cache Paths from YAML files.
 * Each path is identified by a unique ID.
 */
class RacePathManager(private val pathsDirectory: File) {

    private val yaml: Yaml

    private val loadedPaths = ConcurrentHashMap<String, RacePath>()

    init {
        if (!pathsDirectory.exists()) {
            pathsDirectory.mkdirs()
        }

        val dumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        dumperOptions.isPrettyFlow = true

        val loaderOptions = LoaderOptions()
        val constructor = Constructor(PathDto::class.java, loaderOptions)

        val representer = Representer(dumperOptions)
        representer.addClassTag(PathDto::class.java, Tag.MAP)
        representer.addClassTag(PathLocationDto::class.java, Tag.MAP)

        yaml = Yaml(constructor, representer, dumperOptions)

        loadAllPathsAsync()
    }

    /**
     * Performs a complete cleanup of cached memory and reloads all paths from disk asynchronously.
     * Returns a CompletableFuture that completes when the reload process finishes.
     */
    fun reloadAsync(): CompletableFuture<Void> {
        loadedPaths.clear()
        return loadAllPathsAsync()
    }

    /**
     * Saves a Path to a YAML file asynchronusly and updates the memory cache.
     */
    fun savePathAsync(id: String, path: RacePath): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val pathDto = PathDto.fromRacePath(id, path)
                val file = File(pathsDirectory, "$id.yml")

                FileWriter(file).use { writer ->
                    yaml.dump(pathDto, writer)
                }

                loadedPaths[id] = path
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Loads a Path from a YAML file asynchronously.
     * If already cached, returns the cached instance immediately wrapped in a future.
     */
    fun loadPathAsync(id: String): CompletableFuture<RacePath?> {
        val cached = loadedPaths[id]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        return CompletableFuture.supplyAsync {
            try {
                val file = File(pathsDirectory, "$id.yml")
                if (!file.exists()) null
                else {
                    FileInputStream(file).use { inputStream ->
                        val pathDto = yaml.load<PathDto>(inputStream)
                        val racePath = pathDto?.toRacePath()

                        if (racePath != null) {
                            loadedPaths[id] = racePath
                        }
                        racePath
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Loads all existing path files into the cache asynchronously.
     */
    fun loadAllPathsAsync(): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            listPathIds().forEach { id ->
                try {
                    val file = File(pathsDirectory, "$id.yml")
                    if (file.exists()) {
                        FileInputStream(file).use { inputStream ->
                            val pathDto = yaml.load<PathDto>(inputStream)
                            pathDto?.toRacePath()?.let { racePath ->
                                loadedPaths[id] = racePath
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Returns a read-only view of all currently loaded paths in memory.
     */
    fun getLoadedPaths(): Collection<RacePath> {
        return loadedPaths.values
    }

    /**
     * Gets a loaded path from memory by its ID, or null if it hasn't been loaded yet.
     */
    fun getLoadedPath(id: String): RacePath? {
        return loadedPaths[id]
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
     * Deletes a saved path from both the disk and memory cache asynchronously.
     */
    fun deletePathAsync(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                loadedPaths.remove(id)
                val file = File(pathsDirectory, "$id.yml")
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Checks if a path exists on disk.
     */
    fun pathExists(id: String): Boolean {
        return File(pathsDirectory, "$id.yml").exists()
    }
}