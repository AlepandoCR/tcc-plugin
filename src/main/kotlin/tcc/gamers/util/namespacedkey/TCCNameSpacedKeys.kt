package tcc.gamers.util.namespacedkey

import org.bukkit.NamespacedKey

enum class TCCNameSpacedKeys(val key: String, val namespace: String) {

    FLYING_NAUTILUS("flying_nautilus", "tcc"),
    DRAGON_OWNER("flying_nautilus_owner", "tcc"),
    DRAGON_HORN("dragon_horn", "tcc"),
    SOUL_ITEM("soul_item", "tcc"),
    FIXED_VAULT("fixed_vault", "tcc"),

    SOCCER_CLEATS("soccer_cleats", "tcc"),

    PARTICLE_PROJECTOR("particle_projector", "tcc"),
    PROJECTOR_RADIUS("projector_radius", "tcc"),
    PROJECTOR_AMOUNT("projector_amount", "tcc"),
    PROJECTOR_NAME("projector_name", "tcc"),
    SOCCER_BALL("soccer_ball","tcc"),

    RACE_CHECKPOINT("race_checkpoint","tcc");

    fun getNamespacedKey(): NamespacedKey {
        return NamespacedKey(namespace, key)
    }
}