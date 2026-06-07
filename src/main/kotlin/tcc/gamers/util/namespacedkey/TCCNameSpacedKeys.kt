package tcc.gamers.util.namespacedkey

import org.bukkit.NamespacedKey

enum class TCCNameSpacedKeys(val key: String, val namespace: String) {

    FLYING_NAUTILUS("flying_nautilus", "tcc"),
    DRAGON_HORN("dragon_horn", "tcc"),
    SOUL_ITEM("soul_item", "tcc");

    fun getNamespacedKey(): NamespacedKey{
        return NamespacedKey(namespace,key)
    }
}