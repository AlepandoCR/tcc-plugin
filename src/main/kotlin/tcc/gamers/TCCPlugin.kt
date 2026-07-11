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
import tcc.gamers.ball.BallInteractionListener
import tcc.gamers.ball.BallManager
import tcc.gamers.ball.dash.DashInteractionListener
import tcc.gamers.ball.dash.DashManager
import tcc.gamers.ball.physics.KickController
import tcc.gamers.event.skript.SkriptEventRegistry
import tcc.gamers.event.vault.VaultFixListener
import tcc.gamers.item.dragon.DragonHornListener
import tcc.gamers.config.ConfigManager
import tcc.gamers.config.HorseConfig
import tcc.gamers.config.command.BallCommand
import tcc.gamers.config.command.ConfigCommand
import tcc.gamers.config.command.DragonTrainingCommand
import tcc.gamers.config.command.ParticleProjectorCommand
import tcc.gamers.config.command.RaidAdminCommand
import tcc.gamers.config.command.SpawnNautilusCommand
import tcc.gamers.data.DataDrivenManager
import tcc.gamers.horses.command.SpartanHorseCommand
import tcc.gamers.item.particle.ParticleProjectorListener
import tcc.gamers.item.particle.ParticleProjectorManager
import tcc.gamers.race.RaceListener
import tcc.gamers.race.RaceManager
import tcc.gamers.raid.RaidManager
import tcc.gamers.skript.EffectDynamicUpdate
import tcc.gamers.skript.ExpressionMutableValue
import tcc.gamers.skript.SkriptMutableRegistry
import tcc.gamers.tutorials.entities.HiddenPlayerManager
import tcc.gamers.tutorials.listener.TutorialStarter
import tcc.gamers.tutorials.tutorial.TutorialManager
import tcc.gamers.tutorials.tutorial.command.PathCommand
import tcc.gamers.tutorials.tutorial.command.RacePathCommand
import tcc.gamers.tutorials.tutorial.command.TutorialCommand
import tcc.gamers.tutorials.tutorial.manager.PathManager
import tcc.gamers.tutorials.tutorial.manager.RacePathManager
import tcc.gamers.tutorials.tutorial.session.SessionManager
import tcc.gamers.util.StorageFolder
import tcc.gamers.util.hasTutorialTag
import tcc.gamers.permadeath.PermaDeathListener
import tcc.gamers.permadeath.PermaDeathManager
import tcc.permadeath.command.PermaDeathCommand
import java.io.File

class TCCPlugin : JavaPlugin() {

    lateinit var spartanApi: SpartanApi
    lateinit var taxiPathManager: PathManager
    lateinit var racePathManager: RacePathManager
    lateinit var sessionManager: SessionManager
    lateinit var configManager: ConfigManager
    lateinit var skriptMutableRegistry: SkriptMutableRegistry
    lateinit var skriptAddon: SkriptAddon
    lateinit var dataDrivenManager: DataDrivenManager
    lateinit var raidManager: RaidManager
    lateinit var particleProjectorManager: ParticleProjectorManager
    lateinit var mythicApi: MythicBukkit
    lateinit var raceManager: RaceManager
    lateinit var ballManager: BallManager
    lateinit var permaDeathManager: PermaDeathManager
    lateinit var kickController: KickController
    lateinit var dashManager: DashManager

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

        registerListeners(
            RaceListener(this),
            TutorialStarter(this),
            DragonHornListener(this),
            VaultFixListener(this),
            ParticleProjectorListener(this, particleProjectorManager),
            BallInteractionListener(this),
            DashInteractionListener(this),
            PermaDeathListener(this.permaDeathManager)
        )

        registerCommands()

        SkriptEventRegistry.registerAll(skriptAddon)

        raidManager = RaidManager(this)

        particleProjectorManager.startLoop()
    }

    private fun registerCommands() {
        val pathCommand = PathCommand(this, taxiPathManager, sessionManager)
        val spartanHorseCommand = SpartanHorseCommand(this, taxiPathManager)
        val configCommand = ConfigCommand(this)
        val raidCommand = RaidAdminCommand(this)
        val nautilusCommand = SpawnNautilusCommand(this)
        val dragonTrainCommand = DragonTrainingCommand(this)
        val racePathCommand = RacePathCommand(this, racePathManager, sessionManager)
        val ballCommand = BallCommand(this)
        val permaDeathCommand = PermaDeathCommand(this)

        val particle = ParticleProjectorCommand()

        registerCommand("tutorials", TutorialCommand(this))
        registerCommand("path", pathCommand, pathCommand)
        registerCommand("taxi", spartanHorseCommand, spartanHorseCommand)
        registerCommand("tccconfig", configCommand, configCommand)
        registerCommand("raids", raidCommand, raidCommand)
        registerCommand("nauti", nautilusCommand, nautilusCommand)
        registerCommand("dragontrain", dragonTrainCommand, dragonTrainCommand)
        registerCommand("particleprojector",particle)
        registerCommand("racepath", racePathCommand, racePathCommand)
        registerCommand("ball", ballCommand, ballCommand)
        registerCommand("permadeath", permaDeathCommand, permaDeathCommand)
    }

    private fun startManagers() {
        HiddenPlayerManager.start(this)
        ballManager = BallManager(this)
        kickController = KickController(this)
        dashManager = DashManager(this)
        taxiPathManager = PathManager(File(this.dataFolder, StorageFolder.PATHS.folderName))
        racePathManager = RacePathManager(File(this.dataFolder, StorageFolder.RACE_PATHS.folderName))
        sessionManager = SessionManager(this)
        particleProjectorManager = ParticleProjectorManager(this)
        raceManager = RaceManager(this)
        configManager = ConfigManager(this)
        permaDeathManager = PermaDeathManager(this)
        bootConfig()
    }

    private fun bootConfig() {
        permaDeathManager.loadAllAsync()
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
        permaDeathManager.saveAllAsync()
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