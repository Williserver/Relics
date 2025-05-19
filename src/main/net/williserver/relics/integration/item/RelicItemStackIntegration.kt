package net.williserver.relics.integration.item

import net.kyori.adventure.text.Component
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * Integrates relic-specific behavior within an ItemStack by registering appropriate event listeners.
 * This integration handles the customization and lifecycle of ItemStacks that are tied to relics
 * as defined within the provided [RelicSet] and [RelicEventBus].
 *
 * @param instance The plugin instance used for constructing unique namespaced keys for relic metadata.
 * @param relicSet The collection of registered relics and their ownership details.
 * @param bus The event bus used to fire and handle custom relic-related events.
 */
class RelicItemStackIntegrator(instance: Plugin,
                               private val relicSet: RelicSet,
                               private val bus: RelicEventBus) {
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

    /**
     * @return listener that listens for events related to the destruction of relic items.
     */
    fun constructRelicRemoveListener(): Listener =
        object: Listener {

            // TODO: consume event
            // TODO: compost event
            // TODO: furnace burn event

            /**
             * Listens for entity damage events and determines if the entity affected by the event
             * is a relic item. If so, it must have been destroyed and the destroy event is triggered.
             */
            @EventHandler
            fun onDestroyEvent(event: EntityDamageEvent) {
                if (event.entity is Item) {
                    purgeIfRelic((event.entity as Item).itemStack)
                }
            }

            /**
             * Listens for entity despawn events which may refer to a Relic. If a relic is affected, fire event.
             */
            @EventHandler
            fun onItemDespawnEvent(event: ItemDespawnEvent) = purgeIfRelic(event.entity.itemStack)

            /**
             * Handles the event where a player breaks an item. If the broken item is a relic, it will be processed.
             */
            @EventHandler
            fun onPlayerDestroyItem(event: PlayerItemBreakEvent) = purgeIfRelic(event.brokenItem)

            /**
             * Checks if the provided item is recognized as a relic and destroys if so.
             * @param item The item stack to be checked and processed as a relic.
             */
            fun purgeIfRelic(item: ItemStack) {
                Bukkit.broadcast(Component.text("Event fired!"))

                if (!item.hasItemMeta()) {
                    return // The item was not a relic
                }

                val relicName = item.itemMeta.persistentDataContainer.get(relicKey, PersistentDataType.STRING)
                    ?: return // The item was not a relic
                val relic = relicSet.relicNamed(relicName)
                    ?: return // The item is no longer registered as a relic -- this may happen if the listener fires multiple times for the same item.

                // The item is a relic that has just been destroyed.
                bus.fireEvent(RelicEvent.DESTROY, relic)
            }
        }
}

// fun isRelic(item: ItemStack): Boolean = item.itemMeta?.persistentDataContainer?.has(NamespacedKey(relicNamespace, relicNameKey), PersistentDataType.STRING) ?: false