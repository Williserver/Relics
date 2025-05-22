package net.williserver.relics.integration.serverevents

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import io.papermc.paper.event.block.CompostItemEvent
import net.williserver.relics.integration.item.RelicItemStackIntegrator
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.AnvilInventory

/**
 * Listener to handle the destruction of Relic-based item stacks across various game events.
 *
 * @property integrator Handles the removal and deregistration of relics when they are destroyed.
 */
class ServerRelicItemStackDestroyListener(
    private val integrator: RelicItemStackIntegrator
): Listener {
    // TODO: use water
    // TODO: on enchant
    // TODO: on repair
    // TODO: spawn egg

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
            integrator.purgeIfRelic((event.entity as Item).itemStack)
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

    /**
     * Purge each ingredient in a crafting recipe that was a relic.
     */
    @EventHandler
    fun onCraft(event: CraftItemEvent) = event.inventory.matrix.forEach {
        if (it != null) {
            integrator.purgeIfRelic(it)
        }
    }

    /**
     * Destroy a Relic used as fuel for a repair.
     *
     * This fires when:
     * - The third slot of an inventory is clicked
     * - That slot has an item, meaning this is a valid repair
     * - The second slot of the anvil, the fuel slot, has a Relic.
     *
     * If so, the Relic will be consumed and destroyed.
     */
    @EventHandler
    fun onRelicInBottomAnvilSlotDestroy(event: InventoryClickEvent) {
        val fuelSlot = 1
        val resultSlot = 2

        if (event.clickedInventory is AnvilInventory
            && event.slot == resultSlot
            && event.inventory.contents.none { it == null })
        {
            integrator.purgeIfRelic(event.inventory.contents[fuelSlot]!!)
        }
    }

    @EventHandler
    fun onBrewingStandFuel(event: BrewingStandFuelEvent) = integrator.purgeIfRelic(event.fuel)

    @EventHandler
    // Purge all relic bottles, as well as the material on top of the brewing stand.
    fun onPotionCreate(event: BrewEvent) = event.contents.forEach {
        // Be advised -- it may sometimes be null, EVEN IF SPIGOT CLAIMS THIS IS NOT THE CASE!
        if (it != null) {
            integrator.purgeIfRelic(it)
        }
    }

    @EventHandler
    fun onFurnaceSmelt(event: FurnaceSmeltEvent) = integrator.purgeIfRelic(event.source)

    @EventHandler
    fun onFurnaceBurn(event: FurnaceBurnEvent) = integrator.purgeIfRelic(event.fuel)

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) = integrator.purgeIfRelic(event.entity.itemStack)

    @EventHandler
    fun onPlayerConsumeItem(event: PlayerItemConsumeEvent) = integrator.purgeIfRelic(event.item)

    @EventHandler
    fun onCompostItem(event: CompostItemEvent) = integrator.purgeIfRelic(event.item)

    @EventHandler
    fun onPlayerDestroyItem(event: PlayerItemBreakEvent) = integrator.purgeIfRelic(event.brokenItem)
}