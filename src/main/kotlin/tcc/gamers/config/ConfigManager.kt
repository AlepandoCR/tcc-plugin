package tcc.gamers.config

import net.democracycraft.democracyLib.api.config.GeneratedConfig
import tcc.gamers.LiveGameDataManager
import tcc.gamers.PersistenceGameDataManager
import tcc.gamers.TCCPlugin
import tcc.gamers.util.ConfigLoader
import tcc.gamers.util.StorageFolder
import java.util.LinkedHashMap

class ConfigManager(private val plugin: TCCPlugin) {

    val persistenceGameDataManager: PersistenceGameDataManager = PersistenceGameDataManager(plugin)

    val liveGameDataManager: LiveGameDataManager = LiveGameDataManager(
        persistenceGameDataManager,
        plugin
    )

    private val registrations: MutableMap<String, ConfigRegistration<*, *>> = LinkedHashMap()

    private lateinit var horseConfigHandle: ConfigHandle<HorseConfig>

    private lateinit var zombieConfigHandle: ConfigHandle<ZombieConfig>

    fun load(): Int = reloadAll()

    fun reloadAll(): Int {
        var successCount = 0
        registrations.values.forEach { registration ->
            if (registration.reload(plugin)) {
                successCount++
            }
        }

        persistenceGameDataManager.reload()
        liveGameDataManager.reload()

        return successCount
    }

    fun registerHorseConfig(): ConfigHandle<HorseConfig> {
        val handle = register(
            key = "horse",
            folder = StorageFolder.HORSE_CONFIG,
            fileName = "horse.yml",
            generatedClass = GeneratedHorseConfig::class.java,
            defaultConfig = HorseConfig(
                3.5f,
                1.0f,
                "wolfmount",
                1.5,
                "<green>Taxi for %player%",
                40.0,
                20
            ),
            mapper = { generated -> HorseConfig(generated) }
        )
        horseConfigHandle = handle


        return handle
    }

    fun registerZombieConfig(): ConfigHandle<ZombieConfig> {
        val zombieHandle = register(
            key = "zombie",
            folder = StorageFolder.ZOMBIE_CONFIG,
            fileName = "zombie.yml",
            generatedClass = GeneratedZombieConfig::class.java,
            defaultConfig = ZombieConfig(
                1.0f,
                1.0f,
                "zombiemount",
                1.5,
                "<green>Goblin"
            ),
            mapper = { generated -> ZombieConfig(generated) }
        )

        zombieConfigHandle = zombieHandle

        return zombieHandle
    }

    fun <G : GeneratedConfig, T : TCCConfig> register(
        key: String,
        folder: StorageFolder,
        fileName: String,
        generatedClass: Class<G>,
        defaultConfig: T,
        mapper: (G) -> T
    ): ConfigHandle<T> {
        val handle = ConfigHandle(defaultConfig)
        registrations[key] = ConfigRegistration(
            key = key,
            folder = folder,
            fileName = fileName,
            generatedClass = generatedClass,
            defaultConfig = defaultConfig,
            mapper = mapper,
            handle = handle
        )
        return handle
    }

    val horseConfig: HorseConfig
        get() = horseConfigHandle.current

    val zombieConfig: ZombieConfig
        get() = zombieConfigHandle.current

    class ConfigHandle<T : TCCConfig>(initial: T) {
        @Volatile
        var current: T = initial
            internal set
    }

    private class ConfigRegistration<G : GeneratedConfig, T : TCCConfig>(
        private val key: String,
        private val folder: StorageFolder,
        private val fileName: String,
        private val generatedClass: Class<G>,
        private val defaultConfig: T,
        private val mapper: (G) -> T,
        private val handle: ConfigHandle<T>
    ) {
        fun reload(plugin: TCCPlugin): Boolean {
            return try {
                var loadedConfig: T? = null
                ConfigLoader(plugin)
                    .loadAndTransform(
                        folder,
                        fileName,
                        generatedClass,
                        mapper,
                        { config -> loadedConfig = config },
                        key
                    )
                    .loadAll()
                    .join()

                handle.current = loadedConfig ?: defaultConfig
                true
            } catch (ex: Exception) {
                plugin.logger.warning("Failed to load $key config, using defaults: ${ex.message}")
                handle.current = defaultConfig
                false
            }
        }
    }
}



