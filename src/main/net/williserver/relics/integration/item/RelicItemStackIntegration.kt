package net.williserver.relics.integration.item

import net.williserver.relics.model.Relic
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

const val relicNamespace = "relics"
const val relicNameKey = "relicName"

/**
 * Registers a given item as a relic by modifying its metadata to reflect the properties of the relic.
 *
 * @param startingItem The base `ItemStack` to be converted into a relic. Must contain exactly one item.
 * @param relic The `Relic` containing the name and rarity to apply to the item.
 * @return A new `ItemStack` instance configured as a relic.
 * @throws IllegalArgumentException If the `startingItem` contains more than one item.
 */
fun registerItemAsRelic(startingItem: ItemStack, relic: Relic): ItemStack {
    if (startingItem.amount != 1) {
        throw IllegalArgumentException("Relic itemstack should contain only one item!")
    }

    val item = startingItem.clone()
    item.itemMeta = item.itemMeta.apply {
        setDisplayName("${relic.rarity} ${relic.name}")
        persistentDataContainer.set(NamespacedKey(relicNamespace, relicNameKey), PersistentDataType.STRING, relic.name)
    }
    return item
}