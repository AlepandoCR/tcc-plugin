package tcc.gamers.config.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import tcc.gamers.TCCPlugin
import tcc.gamers.util.ChestBuilder
import tcc.gamers.util.PermissionNode
import tcc.gamers.util.hasPermission
import tcc.gamers.util.sendComponent

class RaidAdminCommand(
    private val plugin: TCCPlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(PermissionNode.RAID_ADMIN)) {
            sender.sendComponent(Component.text("You don't have permission to manage raids.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendComponent(Component.text("Usage: /raidadmin <action> [raid-id]").color(NamedTextColor.YELLOW))
            return true
        }

        val action = args[0].lowercase()

        if (action == "startall") {
            val templates = plugin.raidManager.availableRaidTemplateCopies
            if (templates.isEmpty()) {
                sender.sendComponent(Component.text("No available raid templates to start.").color(NamedTextColor.YELLOW))
                return true
            }
            templates.forEach { it.start() }
            sender.sendComponent(Component.text("Started all ${templates.size} available raids.").color(NamedTextColor.GREEN))
            return true
        }

        if (action == "stopall") {
            val activeRaids = plugin.raidManager.activeRaids
            if (activeRaids.isEmpty()) {
                sender.sendComponent(Component.text("No active raids are running.").color(NamedTextColor.YELLOW))
                return true
            }
            val count = activeRaids.size
            ArrayList(activeRaids).forEach {
                it.stopRaidTickLogic()
                it.stop()
            }
            sender.sendComponent(Component.text("Stopped all $count active raids.").color(NamedTextColor.GREEN))
            return true
        }

        // maps — da todos los mapas de todas las raids empacados en cofres recursivos
        if (action == "maps") {
            if (sender !is Player) {
                sender.sendComponent(Component.text("Only players can receive maps.").color(NamedTextColor.RED))
                return true
            }
            val maps = plugin.raidManager.maps
            if (maps.isEmpty()) {
                sender.sendComponent(Component.text("No raid templates with maps available.").color(NamedTextColor.YELLOW))
                return true
            }
            val chest = ChestBuilder.pack(maps)
            sender.inventory.addItem(chest)
            sender.sendComponent(Component.text("You received a chest with all ${maps.size} raid maps.").color(NamedTextColor.GREEN))
            return true
        }

        if (args.size < 2) {
            sender.sendComponent(Component.text("Usage: /raidadmin $action <raid-id>").color(NamedTextColor.YELLOW))
            return true
        }

        val raidId = args[1]

        when (action) {
            "start" -> {
                val raid = plugin.raidManager.getRaidTemplateCopy(raidId).orElse(null)
                if (raid == null) {
                    sender.sendComponent(Component.text("Raid template '$raidId' not found.").color(NamedTextColor.RED))
                    return true
                }
                raid.start()
                sender.sendComponent(Component.text("Raid '$raidId' has been started.").color(NamedTextColor.GREEN))
            }
            "stop" -> {
                val activeRaids = plugin.raidManager.getActiveRaidsByIdentifier(raidId)
                if (activeRaids.isEmpty()) {
                    sender.sendComponent(Component.text("No active instances found running for '$raidId'.").color(NamedTextColor.YELLOW))
                    return true
                }
                val count = activeRaids.size
                activeRaids.forEach { it.stop() }
                sender.sendComponent(Component.text("Stopped $count active instance(s) of '$raidId'.").color(NamedTextColor.GREEN))
            }
            "trigger" -> {
                val activeRaids = plugin.raidManager.getActiveRaidsByIdentifier(raidId)
                if (activeRaids.isEmpty()) {
                    sender.sendComponent(Component.text("No active instances found running for '$raidId' to trigger.").color(NamedTextColor.RED))
                    return true
                }
                activeRaids.forEach {
                    it.spawnMobs()
                    it.startRaidTickLogic()
                }
                sender.sendComponent(Component.text("Triggered ${activeRaids.size} active instance(s) of '$raidId'.").color(NamedTextColor.GREEN))
            }
            "map" -> {
                if (sender !is Player) {
                    sender.sendComponent(Component.text("Only players can receive maps.").color(NamedTextColor.RED))
                    return true
                }
                val template = plugin.raidManager.raidTemplates
                    .firstOrNull { it.identifier.equals(raidId, ignoreCase = true) }
                if (template == null) {
                    sender.sendComponent(Component.text("Raid template '$raidId' not found.").color(NamedTextColor.RED))
                    return true
                }
                sender.inventory.addItem(template.mapRenderer.create())
                sender.sendComponent(Component.text("You received the map for raid '$raidId'.").color(NamedTextColor.GREEN))
            }
            else -> {
                sender.sendComponent(Component.text("Unknown action '$action'. Use start, stop, trigger, map, maps, startall, or stopall.").color(NamedTextColor.RED))
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("start", "stop", "trigger", "map", "maps", "startall", "stopall")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> {
                val action = args[0].lowercase()
                if (action == "startall" || action == "stopall" || action == "maps") {
                    emptyList()
                } else {
                    plugin.raidManager.raidTemplates
                        .map { it.identifier }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
            }
            else -> emptyList()
        }
    }
}