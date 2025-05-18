package net.williserver.relics.integration.item

import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

const val relicNameKey = "relicName"

/**
 * @return a relic lifecycle listener that replaces an item without metadata with a new metadata item
 */
fun registerRelicItemStackListener(): RelicLifecycleListener = { relic, creator, relicItem ->
    if (relicItem!!.amount != 1) {
        throw IllegalArgumentException("Relic itemstack should contain only one item!")
    }

    relicItem.editMeta {
        it.setDisplayName("${relic.rarity} ${relic.name}")
        it.persistentDataContainer.set(
            NamespacedKey(Bukkit.getPluginManager().getPlugin("Relics")!!, relicNameKey),
            PersistentDataType.STRING, relic.name)
    }
}

// fun isRelic(item: ItemStack): Boolean = item.itemMeta?.persistentDataContainer?.has(NamespacedKey(relicNamespace, relicNameKey), PersistentDataType.STRING) ?: false