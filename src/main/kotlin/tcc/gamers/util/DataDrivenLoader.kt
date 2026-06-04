package tcc.gamers.util

import net.democracycraft.democracyLib.api.config.ConfigFactory
import net.democracycraft.democracyLib.api.config.GeneratedConfig
import tcc.gamers.TCCPlugin
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.logging.Logger

/**
 * Utility class for loading data-driven plugin configurations from directories.
 * It reads all `.yml` files within a specified folder, parses them into generated configs,
 * transforms them into DTOs, and returns a list of the loaded objects.
 *
 * Example usage:
 * <pre>`DataDrivenLoader loader = new DataDrivenLoader(plugin);
 * loader.loadDirectory(
 * StorageFolder.RAIDS,
 * GeneratedRaidDto.class,
 * config -> new RaidDto(config),
 * raids -> {
 * this.raidsMap = raids.stream().collect(Collectors.toMap(RaidDto::getAreaIdentifier, r -> r));
 * },
 * "RaidData"
 * );
 * CompletableFuture<Void> allLoaded = loader.loadAll();`</pre>
 */
@Suppress("unused")
class DataDrivenLoader(private val plugin: TCCPlugin) {
    private val logger: Logger = plugin.logger
    private val tasks: MutableList<DirectoryLoadTask<*, *>> = ArrayList()

    /**
     * Registers a directory to be loaded and transformed.
     *
     * @param folder         The folder to scan for .yml files
     * @param generatedClass The generated config class to load for each file
     * @param transformer    Function to transform the generated config to the target DTO
     * @param onLoad         Callback executed with the list of successfully loaded and transformed configs
     * @param <G>            The generated config type
     * @param <T>            The transformed DTO type
     * @return This loader for chaining
     */
    fun <G : GeneratedConfig, T : Any> loadDirectory(
        folder: StorageFolder,
        generatedClass: Class<G>,
        transformer: Function<G, T>,
        onLoad: Consumer<List<T>>
    ): DataDrivenLoader {
        tasks.add(
            DirectoryLoadTask(
                folder,
                generatedClass,
                transformer,
                onLoad,
                null
            )
        )
        return this
    }

    /**
     * Registers a directory to be loaded and transformed, with a specific name for logging.
     *
     * @param folder         The folder to scan for .yml files
     * @param generatedClass The generated config class to load for each file
     * @param transformer    Function to transform the generated config to the target DTO
     * @param onLoad         Callback executed with the list of successfully loaded and transformed configs
     * @param configName     Human-readable name for logging purposes (e.g., "Raids", "Areas")
     * @param <G>            The generated config type
     * @param <T>            The transformed DTO type
     * @return This loader for chaining
     */
    fun <G : GeneratedConfig, T : Any> loadDirectory(
        folder: StorageFolder,
        generatedClass: Class<G>,
        transformer: Function<G, T>,
        onLoad: Consumer<List<T>>,
        configName: String
    ): DataDrivenLoader {
        tasks.add(DirectoryLoadTask(folder, generatedClass, transformer, onLoad, configName))
        return this
    }

    /**
     * Scans all registered directories and loads their configurations asynchronously.
     *
     * @return A CompletableFuture that completes when all directories and their files are loaded
     */
    fun loadAll(): CompletableFuture<Void> {
        logger.info("Starting data-driven config loading...")

        val taskFutures = tasks.map { task ->
            val dir = task.folder.getFolder(plugin)
            val displayName = task.configName ?: task.generatedClass.simpleName

            // Ensure directory exists
            if (!dir.exists()) {
                dir.mkdirs()
            }

            // Find all YAML files in the directory
            val files = dir.listFiles { _, name -> name.endsWith(".yml") } ?: emptyArray()

            if (files.isEmpty()) {
                logger.info("No files found for data-driven config: $displayName")
                @Suppress("UNCHECKED_CAST")
                (task.onLoad as Consumer<List<Any>>).accept(emptyList())
                return@map CompletableFuture.completedFuture<Void>(null)
            }

            // Map each file to a future that loads and transforms it
            val fileFutures = files.map { file ->
                @Suppress("UNCHECKED_CAST")
                ConfigFactory
                    .loadOrCreateAsync(file, task.generatedClass as Class<GeneratedConfig>)
                    .thenApply { config ->
                        try {
                            (task.transformer as Function<GeneratedConfig, Any>).apply(config)
                        } catch (ex: Exception) {
                            logger.severe("Failed to transform file ${file.name} for $displayName: ${ex.message}")
                            null // Return null on transform failure to filter out later
                        }
                    }
                    .exceptionally { ex ->
                        logger.severe("Failed to load file ${file.name} for $displayName: ${ex.message}")
                        null // Return null on read failure
                    }
            }

            // Wait for all files in this specific directory to finish loading
            CompletableFuture.allOf(*fileFutures.toTypedArray()).thenAccept {
                // Filter out any files that failed to load/transform (the nulls)
                val loadedItems = fileFutures.mapNotNull { it.join() }

                @Suppress("UNCHECKED_CAST")
                (task.onLoad as Consumer<List<Any>>).accept(loadedItems)

                logger.info("Loaded ${loadedItems.size} items for $displayName.")
            }
        }

        // Wait for all directory tasks to finish
        return CompletableFuture.allOf(*taskFutures.toTypedArray())
            .thenRun { logger.info("All data-driven configurations loaded successfully.") }
    }

    /**
     * Internal data class representing a pending directory load task.
     */
    private class DirectoryLoadTask<G : GeneratedConfig, T : Any>(
        val folder: StorageFolder,
        val generatedClass: Class<G>,
        val transformer: Function<G, T>,
        val onLoad: Consumer<List<T>>,
        val configName: String?
    )
}