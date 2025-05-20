package net.williserver.relics.integration.item

import io.papermc.paper.event.block.CompostItemEvent
import net.kyori.adventure.text.Component
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

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
class RelicItemStackIntegrator(private val instance: Plugin,
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

    /***
     * @param item The item stack to be checked for relic status.
     * @return Whether the itemstack has relic metadata.
     */
    fun isRelic(item: ItemStack) = item.itemMeta.persistentDataContainer.has(relicKey, PersistentDataType.STRING)

    /**
     * @return listener that listens for events related to the destruction of relic items.
     */
    fun constructRelicRemoveListener() = object: Listener {
        /**
         * - Problem: items have a HIDDEN hitpoints field. Only when that reaches zero should they be destroyed.
         * And partial damage can be done, i.e. by out of radius blasts -- see https://www.spigotmc.org/threads/how-to-tell-if-an-item-is-destroyed.518287/page-2
         * - Solution: track the number of times an item has been damaged and destroy it when it reaches five, the number of hitpoints.
         */
        private val itemsToDamage = mutableMapOf<Item, UInt>()

        /**
         * Registers a task to reset the amount of damage taken by an item.
         * The idea here is that after a single tick, if the item has not been damaged, we reset it from tracking.
         *
         *
         * MAXIMUM_HP ticks, the item should no longer have damage tracked
         * Either it's taken enough damage to be destroyed, or the damage stopped, after which point its hitpoints immediately reset.
         * @param itemToResetDamageOf The item to be reset.
         * @param starterDamage damage when the task was registered.
         */
        private inner class ItemDamageResetTask(val itemToResetDamageOf: Item, val starterDamage: UInt): BukkitRunnable() {
            override fun run() {
                // No damage was dealt in the last tick!
                if (itemsToDamage[itemToResetDamageOf] == starterDamage) {
                    itemsToDamage.remove(itemToResetDamageOf)
                    Bukkit.broadcast(Component.text("Reset damage total!"))
                }
            }

            /**
             * Starts the task, waiting a single tick to see if the item took more damage.
             * If the item has not been damaged, the damage total is reset.
             *
             * NOTE: using a delay of two ticks because synchronous Bukkit tasks have priority over the damage listener.
             * THIS CODE CANNOT YET BE SAFELY THREADED -- needs good investigation, perhaps with semaphore?
             */
            fun checkAndResetDamageNextTick() {
                this.runTaskLater(instance, 2L)
            }
        }

        // TODO: use water
        // TODO: on potion throw -- including bottle 'o enchanting
        // TODO: on enchant
        // TODO: potion material on top of brewing stand.

        /**
         * Listens for entity damage events and determines if the entity affected by the event
         * is a relic item. If so, it must have been destroyed and the destroy event is triggered.
         */
        @EventHandler
        fun onDamageEvent(event: EntityDamageEvent) {
            // Avoid further computation on non-relic items.
            Bukkit.broadcast(Component.text("Item took damage!"))
            if (event.entity !is Item) {
                return
            }
            val item = event.entity as Item
            if (!isRelic(item.itemStack)) {
                return
            }

            // Tracking hidden item hitpoints. See https://www.spigotmc.org/threads/how-to-tell-if-an-item-is-destroyed.518287/page-2
            itemsToDamage[item] = itemsToDamage.getOrDefault(item, 0u) + 1u
            val damageDealtSoFar = itemsToDamage[item]!!

            if (damageDealtSoFar >= ITEM_HITPOINTS) {
                /*
                 * Enough damage has been dealt to destroy the item completely!
                 */
                Bukkit.broadcast(Component.text("Destroying item!"))
                purgeIfRelic(item.itemStack)
            }

            ItemDamageResetTask(item, itemsToDamage[item]!!).checkAndResetDamageNextTick()
        }

        /*
         * Listeners for various ways of destroying an item.
         */

        @EventHandler
        fun onBrewingStandFuel(event: BrewingStandFuelEvent) = purgeIfRelic(event.fuel)

        @EventHandler
        // Purge all relic bottles.
        fun onPotionCreate(event: BrewEvent) = event.contents.forEach {
            // Be advised -- it may sometimes be null, EVEN IF SPIGOT CLAIMS THIS IS NOT THE CASE!
            if (it != null) {
                purgeIfRelic(it)
            }
        }

        @EventHandler
        fun onFurnaceSmelt(event: FurnaceSmeltEvent) = purgeIfRelic(event.source)

        @EventHandler
        fun onFurnaceBurn(event: FurnaceBurnEvent) = purgeIfRelic(event.fuel)

        @EventHandler
        fun onItemDespawn(event: ItemDespawnEvent) = purgeIfRelic(event.entity.itemStack)

        @EventHandler
        fun onPlayerConsumeItem(event: PlayerItemConsumeEvent) = purgeIfRelic(event.item)

        @EventHandler
        fun onCompostItem(event: CompostItemEvent) = purgeIfRelic(event.item)

        @EventHandler
        fun onPlayerDestroyItem(event: PlayerItemBreakEvent) = purgeIfRelic(event.brokenItem)

        /*
         * Internal helpers
         */

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

    companion object {
        /**
         * Items have a hidden hitpoints field, and only when an item is damaged five times should it be destroyed.
         * See: https://www.spigotmc.org/threads/how-to-tell-if-an-item-is-destroyed.518287/page-2
         */
        private const val ITEM_HITPOINTS = 5u
    }
}