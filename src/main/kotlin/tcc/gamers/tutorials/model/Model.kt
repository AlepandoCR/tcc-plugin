package tcc.gamers.tutorials.model

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import kr.toxicity.model.api.platform.PlatformBillboard
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import tcc.gamers.tutorials.model.preset.ModelPreset
import tcc.gamers.tutorials.model.util.ModelManager
import tcc.gamers.util.hideAllExcept
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import tcc.gamers.tutorials.model.util.RendererSupplier.resize

class Model<T : Entity> private constructor(
    private val name: String,
    val entity: T,
    private val tracker: EntityTracker
) {
    init {
        ModelManager.register(this)
    }

    fun hideToAllExcept(player: Player) {
        // Hide the underlying entity from all except the given player
        entity.hideAllExcept(player)
        // Also restrict the model to only spawn for that player
        BetterModel.player(player.uniqueId).ifPresent { tracker.markPlayerForSpawn(it.player()) }
    }

    fun animate(animationName: String) {
        tracker.animate(animationName, AnimationModifier.DEFAULT)
    }

    fun rotate(x: Float, y: Float) {
        tracker.rotation { ModelRotation(x, y) }
    }

    fun scale(value: Double) {
        tracker.scaler(ModelScaler.value(value.toFloat()))
    }

    fun resetScale() {
        tracker.scaler(ModelScaler.entity())
    }

    fun scaleWithEntity(value: Double) {
        if (entity !is LivingEntity) {
            scale(value)
            return
        }
        (entity as LivingEntity).resize(value)
        tracker.scaler(ModelScaler.entity())
    }

    fun teleport(location: Location) {
        entity.teleport(location)
    }

    fun damageTint(color: org.bukkit.Color) {
        tracker.update(TrackerUpdateAction.tint(color.asRGB()))
    }

    fun despawn() {
        ModelManager.unregister(this)
        tracker.close()   // closes the BetterModel tracker and despawns all display entities
        entity.remove()
    }

    fun hide(player: Player) {
        BetterModel.player(player.uniqueId).ifPresent { tracker.hide(it.player()) }
    }

    fun getLocation(): Location = entity.location

    fun setGlowing(rgb: Int) {
        tracker.update(TrackerUpdateAction.glow(true))
        tracker.update(TrackerUpdateAction.glowColor(rgb))
    }

    fun stopGlowing() {
        tracker.update(TrackerUpdateAction.glow(false))
    }

    fun enchant() {
        tracker.update(TrackerUpdateAction.enchant(true))
    }

    fun unenchant() {
        tracker.update(TrackerUpdateAction.enchant(false))
    }

    fun billboard(billboard: org.bukkit.entity.Display.Billboard) {
        val platformBillboard = when (billboard) {
            org.bukkit.entity.Display.Billboard.FIXED      -> PlatformBillboard.FIXED
            org.bukkit.entity.Display.Billboard.VERTICAL   -> PlatformBillboard.VERTICAL
            org.bukkit.entity.Display.Billboard.HORIZONTAL -> PlatformBillboard.HORIZONTAL
            org.bukkit.entity.Display.Billboard.CENTER     -> PlatformBillboard.CENTER
        }
        tracker.update(TrackerUpdateAction.billboard(platformBillboard))
    }

    companion object {
        @JvmStatic
        fun <A : Entity> fromPreset(preset: ModelPreset<A>, location: Location): Model<A> {
            return preset.buildModel(location)
        }

        /** Spawn a new backing entity and attach the model to it. */
        @JvmStatic
        fun <A : Entity> spawn(name: String, location: Location, entityClass: Class<A>): Model<A> {
            val renderer = BetterModel.model(name)
                .orElseThrow { IllegalArgumentException("Model '$name' not found in BetterModel") }
            val entity = location.world.spawn(location, entityClass)
            val betterModelEntity = BukkitAdapter.adapt(entity)
            val tracker = renderer.create(betterModelEntity)
            return Model(name, entity, tracker)
        }

        /** Attach the model to an existing entity without respawning it. */
        @JvmStatic
        fun <A : Entity> attachTo(name: String, existing: A): Model<A> {
            val renderer = BetterModel.model(name)
                .orElseThrow { IllegalArgumentException("Model '$name' not found in BetterModel") }
            val betterModelEntity = BukkitAdapter.adapt(existing)
            val tracker = renderer.create(betterModelEntity)
            return Model(name, existing, tracker)
        }
    }
}