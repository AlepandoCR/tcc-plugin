package tcc.gamers.tutorials.model.util

import tcc.gamers.tutorials.model.Model
import org.bukkit.Location
import org.bukkit.entity.Entity

@Suppress("unused")
fun <T : Entity> T.attachModel(name: String): Model<T> {
    return Model.attachTo(name, this)
}

object ModelManager {
    private val repo = mutableListOf<Model<out Entity>>()

    fun register(vararg model: Model<out Entity>){
        repo.addAll(model)
    }

    @Suppress("unused")
    inline fun <reified T : Entity> modelOf(name: String, location: Location): Model<T> {
        return Model.spawn(name, location, T::class.java)
    }

    fun <T: Entity> Class<T>.buildModel(name: String, location: Location): Model<T> {
        return Model.spawn(name, location, this)
    }

    @JvmStatic
    fun <T : Entity> attachModel(entity: T, name: String): Model<T> {
        return entity.attachModel(name)
    }

    @Suppress("unused")
    fun Entity.getModel(): Model<out Entity>?{
        repo.forEach {
            if(it.entity == this){
                return it
            }
        }

        return null
    }

    fun unregister(vararg model: Model<out Entity>){
        repo.removeAll(model.toSet())
    }
}