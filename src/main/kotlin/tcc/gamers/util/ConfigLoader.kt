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
 * Utility class for loading plugin configurations in a streamlined manner.
 * Reduces boilerplate code when loading multiple configuration files.
 *
 *
 * Example usage:
 * <pre>`ConfigLoader loader = new ConfigLoader(plugin); loader     .load(ConfigFolder.GAME_CONFIGS, "config.yml", GeneratedConfig.class, config -> {         this.myConfig = new MyConfig(config);     })     .load(ConfigFolder.TOOL_CONFIGS, "another.yml", GeneratedAnother.class, config -> {         this.anotherConfig = new AnotherConfig(config);     }); CompletableFuture<Void> allLoaded = loader.loadAll(); `</pre>
 */
@Suppress("unused")
class ConfigLoader(private val plugin: TCCPlugin) {
    private val logger: Logger = plugin.logger
    private val tasks: MutableList<ConfigLoadTask<*>> = ArrayList()

    /**
     * Registers a configuration to be loaded.
     *
     * @param folder         The folder where the config file is located
     * @param fileName       The name of the config file
     * @param generatedClass The generated config class to load
     * @param onLoad         Callback executed when the config is successfully loaded
     * @param <G>            The generated config type
     * @return This loader for chaining
    </G> */
    fun <G : GeneratedConfig> load(
        folder: StorageFolder,
        fileName: String,
        generatedClass: Class<G>,
        onLoad: Consumer<G>
    ): ConfigLoader {
        tasks.add(ConfigLoadTask(folder, fileName, generatedClass, onLoad, null))
        return this
    }

    /**
     * Registers a configuration to be loaded with a config name for logging.
     *
     * @param folder         The folder where the config file is located
     * @param fileName       The name of the config file
     * @param generatedClass The generated config class to load
     * @param onLoad         Callback executed when the config is successfully loaded
     * @param configName     Human-readable name for logging purposes
     * @param <G>            The generated config type
     * @return This loader for chaining
    </G> */
    fun <G : GeneratedConfig> load(
        folder: StorageFolder,
        fileName: String,
        generatedClass: Class<G>,
        onLoad: Consumer<G>,
        configName: String
    ): ConfigLoader {
        tasks.add(ConfigLoadTask(folder, fileName, generatedClass, onLoad, configName))
        return this
    }

    /**
     * Registers a configuration with a transformer that converts the generated config.
     *
     * @param folder         The folder where the config file is located
     * @param fileName       The name of the config file
     * @param generatedClass The generated config class to load
     * @param transformer    Function to transform the generated config to the target type
     * @param onLoad         Callback executed with the transformed config
     * @param <G>            The generated config type
     * @param <T>            The transformed config type
     * @return This loader for chaining
    </T></G> */
    fun <G : GeneratedConfig, T : Any> loadAndTransform(
        folder: StorageFolder,
        fileName: String,
        generatedClass: Class<G>,
        transformer: Function<G, T>,
        onLoad: Consumer<T>
    ): ConfigLoader {
        tasks.add(ConfigLoadTask(folder, fileName, generatedClass, { config: G -> onLoad.accept(transformer.apply(config)) }, null))
        return this
    }

    /**
     * Registers a configuration with a transformer and a config name for logging.
     *
     * @param folder         The folder where the config file is located
     * @param fileName       The name of the config file
     * @param generatedClass The generated config class to load
     * @param transformer    Function to transform the generated config to the target type
     * @param onLoad         Callback executed with the transformed config
     * @param configName     Human-readable name for logging purposes
     * @param <G>            The generated config type
     * @param <T>            The transformed config type
     * @return This loader for chaining
    </T></G> */
    fun <G : GeneratedConfig, T : Any> loadAndTransform(
        folder: StorageFolder,
        fileName: String,
        generatedClass: Class<G>,
        transformer: Function<G, T>,
        onLoad: Consumer<T>,
        configName: String
    ): ConfigLoader {
        tasks.add(ConfigLoadTask(folder, fileName, generatedClass, { config: G -> onLoad.accept(transformer.apply(config)) }, configName))
        return this
    }

    /**
     * Loads all registered configurations asynchronously.
     *
     * @return A CompletableFuture that completes when all configs are loaded
     */
    fun loadAll(): CompletableFuture<Void> {
        logger.info("Starting config loading...")

        val futures = ArrayList<CompletableFuture<Void>>(tasks.size)

        for (task in tasks) {
            val configFile = File(task.folder.getFolder(plugin), task.fileName)
            val displayName = task.configName ?: task.fileName

            @Suppress("UNCHECKED_CAST")
            val future = ConfigFactory
                .loadOrCreateAsync<GeneratedConfig>(
                    configFile,
                    task.generatedClass as Class<GeneratedConfig>
                )
                .thenAccept { config ->
                    @Suppress("UNCHECKED_CAST")
                    (task.onLoad as Consumer<GeneratedConfig>).accept(config)
                    logger.info("$displayName config loaded.")
                }
                .whenComplete { _, ex ->
                    if (ex != null) {
                        logger.severe("Failed to load $displayName config: ${ex.message}")
                    }
                }

            futures.add(future)
        }

        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenRun { logger.info("All configurations loaded successfully.") }
    }

    private class ConfigLoadTask<G : GeneratedConfig>(
        val folder: StorageFolder,
        val fileName: String,
        val generatedClass: Class<G>,
        val onLoad: Consumer<G>,
        val configName: String?
    )
}