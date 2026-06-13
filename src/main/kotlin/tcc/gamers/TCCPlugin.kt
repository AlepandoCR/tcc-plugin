package tcc.gamers

import ch.njol.skript.Skript
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.addon.SkriptAddon
import org.spartan.api.SpartanApi
import org.spartan.internal.facade.SpartanApiImpl
import tcc.gamers.ai.event.skript.SkriptEventRegistry
import tcc.gamers.ai.event.vault.VaultFixListener
import tcc.gamers.item.dragon.DragonHornListener
import tcc.gamers.config.ConfigManager
import tcc.gamers.config.HorseConfig
import tcc.gamers.config.command.ConfigCommand
import tcc.gamers.config.command.DragonTrainingCommand
import tcc.gamers.config.command.RaidAdminCommand
import tcc.gamers.config.command.SpawnNautilusCommand
import tcc.gamers.data.DataDrivenManager
import tcc.gamers.horses.command.SpartanHorseCommand
import tcc.gamers.raid.RaidManager
import tcc.gamers.skript.EffectDynamicUpdate
import tcc.gamers.skript.ExpressionMutableValue
import tcc.gamers.skript.SkriptMutableRegistry
import tcc.gamers.tutorials.entities.HiddenPlayerManager
import tcc.gamers.tutorials.listener.TutorialStarter
import tcc.gamers.tutorials.tutorial.TutorialManager
import tcc.gamers.tutorials.tutorial.command.PathCommand
import tcc.gamers.tutorials.tutorial.command.TutorialCommand
import tcc.gamers.tutorials.tutorial.manager.PathManager
import tcc.gamers.tutorials.tutorial.session.SessionManager
import tcc.gamers.util.StorageFolder
import tcc.gamers.util.hasTutorialTag
import java.io.File

class TCCPlugin : JavaPlugin() {

    lateinit var spartanApi: SpartanApi
    lateinit var pathManager: PathManager
    lateinit var sessionManager: SessionManager
    lateinit var configManager: ConfigManager
    lateinit var skriptMutableRegistry: SkriptMutableRegistry
    lateinit var skriptAddon: SkriptAddon
    lateinit var dataDrivenManager: DataDrivenManager
    lateinit var raidManager: RaidManager
    lateinit var mythicApi: MythicBukkit

    companion object {
        @JvmStatic
        lateinit var instance: TCCPlugin
            private set
    }

    val horseConfig: HorseConfig
        get() = configManager.horseConfig

    override fun onLoad() {
        instance = this
        dataDrivenManager = DataDrivenManager(this)
    }

    override fun onEnable() {
        mythicApi = MythicBukkit.inst()
        spartanApi = SpartanApiImpl()
        startSkriptApi()
        
        skriptMutableRegistry = SkriptMutableRegistry()
        if (!this.dataFolder.exists()) this.dataFolder.mkdirs()
        startManagers()

        registerCommands()

        registerListeners(
            TutorialStarter(this),
            DragonHornListener(this),
            VaultFixListener(this)
        )

        SkriptEventRegistry.registerAll(skriptAddon)

        raidManager = RaidManager(this)
    }

    private fun registerCommands() {
        val pathCommand = PathCommand(this, pathManager, sessionManager)
        val spartanHorseCommand = SpartanHorseCommand(this, pathManager)
        val configCommand = ConfigCommand(this)
        val raidCommand = RaidAdminCommand(this)
        val nautilusCommand = SpawnNautilusCommand(this)
        val dragonTrainCommand = DragonTrainingCommand(this)

        registerCommand("tutorials", TutorialCommand(this))
        registerCommand("path", pathCommand, pathCommand)
        registerCommand("taxi", spartanHorseCommand, spartanHorseCommand)
        registerCommand("tccconfig", configCommand, configCommand)
        registerCommand("raids", raidCommand, raidCommand)
        registerCommand("nauti", nautilusCommand, nautilusCommand)
        registerCommand("dragontrain", dragonTrainCommand, dragonTrainCommand)
    }

    private fun startManagers() {
        HiddenPlayerManager.start(this)
        pathManager = PathManager(File(this.dataFolder, StorageFolder.PATHS.folderName))
        sessionManager = SessionManager(this)
        bootConfig()
    }

    private fun bootConfig() {
        configManager = ConfigManager(this)
        configManager.registerHorseConfig()
        configManager.registerZombieConfig()
        configManager.load()
    }

    private fun startSkriptApi() {
        skriptAddon = Skript.instance().registerAddon(TCCPlugin::class.java, "tcc")

        skriptAddon.let {
            EffectDynamicUpdate.register(it)
            ExpressionMutableValue.register(it)
        }
    }

    private fun registerCommand(commandName: String, executor: CommandExecutor) {
        this.getCommand(commandName)!!.setExecutor(executor)
    }

    private fun registerCommand(commandName: String, executor: CommandExecutor, completer: TabCompleter) {
        this.getCommand(commandName)!!.setExecutor(executor)
        this.getCommand(commandName)!!.tabCompleter = completer
    }

    private fun registerListeners(vararg listeners: Listener) {
        listeners.forEach { server.pluginManager.registerEvents(it, this) }
    }


    override fun onDisable() {
        HiddenPlayerManager.stop()
        TutorialManager.cancelAll()
        // stop active sessions to ensure cleanup
        try { sessionManager.stopAll() } catch (_: Exception) {}
        removeTutorialEntities()
    }

    private fun removeTutorialEntities() {
        Bukkit.getWorlds()
            .asSequence()
            .flatMap { it.entities }
            .filter { it.hasTutorialTag() }
            .forEach { it.remove() }
    }

}