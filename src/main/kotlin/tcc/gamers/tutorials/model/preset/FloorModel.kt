package tcc.gamers.tutorials.model.preset

import org.bukkit.entity.BlockDisplay

object FloorModel: ModelPreset<BlockDisplay> {
    override fun classT(): Class<BlockDisplay> {
       return BlockDisplay::class.java
    }

    override fun modelId(): String {
        return "arrow_2"
    }
}