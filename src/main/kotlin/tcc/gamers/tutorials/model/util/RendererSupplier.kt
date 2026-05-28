package tcc.gamers.tutorials.model.util

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.ModelRenderer
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

object RendererSupplier {

    fun get(name: String): ModelRenderer {
        return BetterModel.model(name)
            .orElseThrow { IllegalArgumentException("Model '$name' not found in BetterModel") }
    }

    fun LivingEntity.resize(value: Double) {
        val attribute = this.getAttribute(Attribute.SCALE)
        attribute?.let {
            it.baseValue = value
        }
    }
}