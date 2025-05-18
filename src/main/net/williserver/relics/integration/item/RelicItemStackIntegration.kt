package net.williserver.relics.integration.item

import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * Integrates custom relic item stack behavior for a plugin in a Minecraft server environment.
 *
 * @param instance The instance of the plugin utilizing this integrator.
 */
class RelicItemStackIntegrator(instance: Plugin) {
    /**
     * This key is used to persist and retrieve relic-specific data
     * (such as the relic name) within the item's metadata.
     */
    private val relicKey = NamespacedKey(instance, "relicName")

    /**
     * @return A [RelicLifecycleListener] that processes and customizes the relic item stack's metadata.
     */
    fun constructRegisterItemStackListener(): RelicLifecycleListener = { relic, creator, relicItem ->
        if (relicItem!!.amount != 1) {
            throw IllegalArgumentException("Relic itemstack should contain only one item!")
        }

        relicItem.editMeta {
            it.setDisplayName("${relic.rarity} ${relic.name}")
            it.persistentDataContainer.set(relicKey, PersistentDataType.STRING, relic.name)
        }
    }
}

// fun isRelic(item: ItemStack): Boolean = item.itemMeta?.persistentDataContainer?.has(NamespacedKey(relicNamespace, relicNameKey), PersistentDataType.STRING) ?: false