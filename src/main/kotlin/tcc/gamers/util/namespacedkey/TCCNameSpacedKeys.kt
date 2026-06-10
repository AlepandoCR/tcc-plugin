package tcc.gamers.util.namespacedkey

import org.bukkit.NamespacedKey

enum class TCCNameSpacedKeys(val key: String, val namespace: String) {

    FLYING_NAUTILUS("flying_nautilus", "tcc"),
    DRAGON_HORN("dragon_horn", "tcc"),
    SOUL_ITEM("soul_item", "tcc"),
    FIXED_VAULT("fixed_vault", "tcc");

    fun getNamespacedKey(): NamespacedKey{
        return NamespacedKey(namespace,key)
    }
}