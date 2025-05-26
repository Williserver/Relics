package net.williserver.relics.integration.serverevents

import io.papermc.paper.event.block.CompostItemEvent
import net.williserver.relics.integration.item.RelicItemStackIntegrator
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.BrewingStandFuelEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
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
    /*
     * Listeners for various ways of destroying an item.
     */

    /**
     * Relics cannot be placed!
     */
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) = event.itemInHand.let {
        if (integrator.hasRelicMetadata(it)
            && it.type !in AXES) {
            event.isCancelled = true
        }
    }

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
        if (event.clickedInventory !is AnvilInventory) return

        val fuelSlot = 1
        val resultSlot = 2

        // If we're completing an anvil transaction WITH THE RELIC AS FUEL, destroy it.
        if (event.slot == resultSlot
            && event.inventory.contents.none { it == null }) {
            integrator.purgeIfRelic(event.inventory.contents[fuelSlot]!!)
        }
    }

    /**
     * Refuse to allow clicking a Relic over to a different inventory.
     */
    @EventHandler
    fun checkMoveRelicAcceptableInventory(event: InventoryClickEvent) {
        // Ignore clicks on empty slots.
        if (event.clickedInventory == null) return

        // It is always OK to move items between acceptable inventories.
        val acceptableInventories = setOf(
            InventoryType.PLAYER,
            InventoryType.CHEST,
            InventoryType.ENDER_CHEST,
            InventoryType.ANVIL,
            InventoryType.ENCHANTING
        )
        if (event.clickedInventory!!.type in acceptableInventories) return

        // Otherwise, check that we haven't placed a relic in an unacceptable inventory.
        if (event.cursor.hasItemMeta() && integrator.hasRelicMetadata(event.cursor)) {
            event.isCancelled = true
        }
    }

    /*
     * Destroy the fuel in an enchantment table if it's a relic.
     * Note that this will be in the second slot -- the first one is the target, which, when enchanted, retains its metadata.
     */
    @EventHandler
    fun onEnchant(event: EnchantItemEvent) = event.inventory.contents[1]?.let { integrator.purgeIfRelic(it) }

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

    /*
     * Even preventing directly placing items in here, it's still possible to use hoppers to get to the target.
     */
    @EventHandler
    fun onFurnaceSmelt(event: FurnaceSmeltEvent) = integrator.purgeIfRelic(event.source)

    @EventHandler
    fun onFurnaceBurn(event: FurnaceBurnEvent) = integrator.purgeIfRelic(event.fuel)

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) = integrator.purgeIfRelic(event.entity.itemStack)

    @EventHandler
    fun onPlayerConsumeItem(event: PlayerItemConsumeEvent) = integrator.purgeIfRelic(event.item)

    @EventHandler
    fun onPlayerDestroyItem(event: PlayerItemBreakEvent) = integrator.purgeIfRelic(event.brokenItem)

    @EventHandler
    fun onCompostItem(event: CompostItemEvent) = integrator.purgeIfRelic(event.item)

    companion object {
        /**
         * Axe materials are in hand when a BlockPlace event is triggered, but should not result in event cancellation.
         */
        val AXES = setOf(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE,
        )
    }
}