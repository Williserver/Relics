package net.williserver.relics.integration.item

import net.kyori.adventure.text.Component
import net.williserver.relics.model.Relic
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.inventory.ItemStack

/**
 * Integrates relic-specific behavior within an ItemStack by registering appropriate event listeners.
 * This integration handles the customization and lifecycle of ItemStacks that are tied to relics
 * as defined within the provided [RelicSet] and [RelicEventBus].
 *
 * @param instance The plugin instance used for constructing unique namespaced keys for relic metadata.
 * @param relicSet The collection of registered relics and their ownership details.
 * @param bus The event bus used to fire and handle custom relic-related events.
 * @author Willmo3
 */
class RelicItemStackIntegrator(instance: Plugin,
                               private val relicSet: RelicSet,
                               private val bus: RelicEventBus) {
    /**
     * This key is used to persist and retrieve relic-specific data
     * (such as the relic name) within the item's metadata.
     */
    private val relicKey = NamespacedKey(instance, "relicName")

    /*
     * Relic accessors
     */

    /**
     * @param item The item stack to be checked for relic status.
     * @return Whether the itemstack has relic metadata.
     */
    fun hasRelicMetadata(item: ItemStack) = item.itemMeta.persistentDataContainer.has(relicKey, PersistentDataType.STRING)

    /**
     * @param item The item stack from which relic metadata is to be retrieved.
     * @return The string associated with the relic in the item's metadata, or null if no relic metadata exists.
     */
    fun relicFromItem(item: ItemStack): Relic {
        if (!hasRelicMetadata(item)) {
            throw IllegalArgumentException("Item is not a relic, this should have been caught earlier!")
        }

        return relicSet.relicNamed(item.itemMeta.persistentDataContainer.get(relicKey, PersistentDataType.STRING)!!)!!
    }

    /*
     * Relic mutators
     */
    /**
     * Checks if the provided item is recognized as a relic and destroys if so.
     * @param item The item stack to be checked and processed as a relic.
     */
    fun purgeIfRelic(item: ItemStack) {
        if (!item.hasItemMeta()) {
            return // The item was not a relic
        }

        val relicName = item.itemMeta.persistentDataContainer.get(relicKey, PersistentDataType.STRING)
            ?: return // The item was not a relic
        val relic = relicSet.relicNamed(relicName)
            ?: return // The item is no longer registered as a relic -- this may happen if the listener fires multiple times for the same item.

        // The item is a relic that has just been destroyed.
        bus.fireEvent(RelicEvent.DESTROY, relic, item = item)
    }

    /*
     * Relic lifecycle listeners
     */

    /**
     * @return A [RelicLifecycleListener] that processes and customizes the relic item stack's metadata.
     */
    fun constructRegisterItemStackListener(): RelicLifecycleListener = { relic, creator, relicItem ->
        if (relicItem!!.amount != 1) {
            throw IllegalArgumentException("Relic itemstack should contain only one item!")
        }

        relicItem.editMeta {
            // Convert to uppercase to match old ChatColor enum name.
            val colorCode = ChatColor.valueOf(relic.rarity.color.toString().uppercase())
            // Prefix with bold code
            it.setDisplayName("$colorCode§l${relic.rarity} ${relic.name}")
            it.persistentDataContainer.set(relicKey, PersistentDataType.STRING, relic.name)
        }
    }

    /**
     * Constructs a lifecycle listener for destroying relic item stacks. When triggered, it processes the given
     * relic item stack to remove relic-specific metadata and reset the display name to its default state.
     *
     * Note that this listener also accepts a null item, in which case an item with relic metadata will still be present in the world.
     * This item cannot, however, be considered a relic.
     *
     * @return A [RelicLifecycleListener] configured to handle the destruction of relic item stacks.
     * @throws IllegalArgumentException if the item stack size is not one, or if the item is not a valid relic.
     */
    fun constructDestroyItemStackListener(): RelicLifecycleListener = { relic, creator, relicItem ->
        // Relic destroy can be invoked separately from an item.
        if (relicItem != null) {
            // But if it is invoked with an item, that item should be a Relic itemstack of size one.
            if (relicItem.amount != 1) {
                throw IllegalArgumentException("Relic itemstack should contain only one item!")
            } else if (!hasRelicMetadata(relicItem)) {
                throw IllegalArgumentException("Item is not a relic and should not have been passed to this listener!")
            } else {
                relicItem.editMeta {
                    it.persistentDataContainer.remove(relicKey)
                    it.setDisplayName("Shattered Relic")
                }
            }
        }
    }

    /*
     * Static helpers
     */
    companion object {
        /**
         * Retrieves the display name of an item. If no custom display name exists,
         * the item's default type name is returned in lowercase with underscores replaced by spaces.
         *
         * @param item The item stack from which the name is to be retrieved.
         * @return The display name of the item or its default type name formatted as a string.
         */
        fun itemName(item: ItemStack) =
            if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
                // Since we're registering relic under plaintext display name, acceptable to use this.
                item.itemMeta!!.displayName
            } else {
                item.type.name.lowercase().replace('_', ' ')
            }

        /**
         * @param player Player to get item in main hand of.
         * @return the ItemStack in the player's main hand.
         */
        fun itemInHand(player: Player): ItemStack? = player.inventory.itemInMainHand
    }
}