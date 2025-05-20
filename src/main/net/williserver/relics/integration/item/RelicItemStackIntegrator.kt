package net.williserver.relics.integration.item

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
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
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
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
    fun constructRelicRemoveListener(): Listener =
        object: Listener {
            // TODO: use water
            // TODO: on enchant

            /*
             * Listeners for various ways of destroying an item.
             */

            /**
             * Listens for entity damage events and determines if the entity affected by the event
             * is a relic item. If so, it must have been destroyed and the destroy event is triggered.
             */
            @EventHandler
            fun onItemDamage(event: EntityDamageEvent) {
                if (event.entity is Item) {

                    purgeIfRelic((event.entity as Item).itemStack)
                }

                /*
                 * A note on item damage.
                 * There is some disagreement in the Spigot community over whether an item is immediately destroyed on taking damage.
                 * In my testing, I have found that even if the damage event fires multiple times, an item WILL be destroyed if it takes even a single damage.
                 * Therefore, I will not be performing any item HP tests -- if it takes damage once, deregister that Relic!
                 *
                 * However, in case I end up being wrong, I've written a prototype solution for handling multi-hitpoint items.
                 * See commit 3613bfd3acd36c24693176e020c91bf501516ae3, which I have now reset.
                 * - Willmo3, 5/20/2025
                 */
            }

            @EventHandler
            fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) = purgeIfRelic(event.itemStack)

            /*
              A note on consumable arrows: these can be picked up again. If not, they'll be caught on despawn.
              If I end up being wrong, here's a quick handler for deregistering arrows when you shoot them.

            @EventHandler
            fun onArrowShoot(event: EntityShootBowEvent) = event.consumable?.let { purgeIfRelic(it) }
             */

            @EventHandler
            fun onBrewingStandFuel(event: BrewingStandFuelEvent) = purgeIfRelic(event.fuel)

            @EventHandler
            // Purge all relic bottles, as well as the material on top of the brewing stand.
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
}

//