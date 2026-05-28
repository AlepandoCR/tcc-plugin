package tcc.gamers.tutorials.model.preset

import tcc.gamers.tutorials.model.Model
import tcc.gamers.tutorials.model.util.ModelManager.buildModel
import org.bukkit.Location
import org.bukkit.entity.Entity

interface ModelPreset<T: Entity> {
    fun buildModel(location: Location): Model<T> {
        return classT().buildModel(modelId(), location)
    }

    fun classT(): Class<T>

    fun modelId(): String
}