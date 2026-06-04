package tcc.gamers.util

import org.bukkit.command.CommandSender
import tcc.gamers.TCCPlugin
import java.io.File

/**
 * Centralized permission nodes used by commands.
 */
@Suppress("unused")
enum class PermissionNode(val node: String) {
    TUTORIALS_START("tutorials.start"),
    PATH_USE("path.use"),
    PATH_CREATE("path.create"),
    PATH_EDIT("path.edit"),
    PATH_LIST("path.list"),
    PATH_CANCEL("path.cancel"),
    PATH_DELETE("path.delete"),
    TAXI_USE("taxi.use"),
    TAXI_SPAWN("taxi.spawn"),
    CONFIG_USE("config.use"),
    CONFIG_RELOAD("config.reload"),
    RAID_ADMIN("raid.admin")
}

fun CommandSender.hasPermission(permission: PermissionNode): Boolean {
    return hasPermission(permission.node)
}

/**
 * Centralized storage folder names used by the plugin.
 */
@Suppress("unused")
enum class StorageFolder(val folderName: String) {
    PATHS("paths"),
    TUTORIALS("tutorials"),
    HORSE_CONFIG("ai/horse"),
    ZOMBIE_CONFIG("ai/zombie"),
    SPARTAN_MODEL("ai/model"),
    GAME_DATA("game_data"),
    CLOUD(GAME_DATA.folderName + "/cloud"),
    TREE(GAME_DATA.folderName + "/tree"),
    TREE_DATA(TREE.folderName + "/tree-game-data.yml"),
    CLOUD_DATA(CLOUD.folderName + "/cloud-game-data.yml"),
    RAID(GAME_DATA.folderName + "/raid"),
    RAID_MOB(RAID.folderName + "/mob"),
    RAID_AREA(RAID.folderName + "/area");

    fun getFolder(plugin: TCCPlugin): File {
        val file = File(plugin.dataFolder, folderName)

        if (folderName.contains(".")) { // its a file
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
        } else {
            if (!file.exists()) file.mkdirs()
        }

        return file
    }
}



