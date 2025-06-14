package net.williserver.relics.integration.item

import net.williserver.relics.model.Relic
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicLifecycleListener
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
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
         * Axe materials are in hand when a BlockPlace event is triggered, but should not result in event cancellation.
         */
        val AXES = setOf(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE,
        )

        /**
         * Inventories that relics can be freely moved between.
         */
        val ACCEPTABLE_INVENTORIES = setOf(
            InventoryType.PLAYER,
            InventoryType.CHEST,
            InventoryType.ENDER_CHEST,
            InventoryType.ANVIL,
            InventoryType.ENCHANTING,
            InventoryType.BARREL,
        )

        /**
         * Set of specifically identified illegal materials.
         *
         * - Arrows do not integrate with Relics.
         * This is because they can be picked up after being shot, BUT the entity that is picked up isn't the same ItemStack.
         * Rather, it's a copy of the arrow entity as it was when it was shot -- including the metadata that should have been deleted!
         * As a result, we can end up with extraneous arrows.
         *
         * - Spawn eggs are illegal because there is no easy event to delete them with, adn they are not semantic relics.
         * It's not worth adding another complex listener for each different type of spawn egg.
         */
        val ILLEGAL_MATERIALS = setOf(
            BARRIER, STRUCTURE_VOID, LEAD,
            // Arrows
            ARROW, SPECTRAL_ARROW, TIPPED_ARROW,
            LEGACY_ARROW, LEGACY_SPECTRAL_ARROW, LEGACY_TIPPED_ARROW,
            // All spawn eggs are illegal!
            // NOTE: creaking spawn egg does not appear supported yet...?
            ALLAY_SPAWN_EGG, ARMADILLO_SPAWN_EGG, AXOLOTL_SPAWN_EGG,
            BAT_SPAWN_EGG, BEE_SPAWN_EGG, BOGGED_SPAWN_EGG,
            BLAZE_SPAWN_EGG, BREEZE_SPAWN_EGG, CAT_SPAWN_EGG,
            CAVE_SPIDER_SPAWN_EGG, CHICKEN_SPAWN_EGG, COD_SPAWN_EGG,
            COW_SPAWN_EGG, CREEPER_SPAWN_EGG, DOLPHIN_SPAWN_EGG,
            DONKEY_SPAWN_EGG, DROWNED_SPAWN_EGG, ELDER_GUARDIAN_SPAWN_EGG,
            ENDERMAN_SPAWN_EGG, ENDERMITE_SPAWN_EGG, EVOKER_SPAWN_EGG,
            FOX_SPAWN_EGG, FROG_SPAWN_EGG, GHAST_SPAWN_EGG,
            GLOW_SQUID_SPAWN_EGG, GUARDIAN_SPAWN_EGG,
            HOGLIN_SPAWN_EGG, HORSE_SPAWN_EGG, HUSK_SPAWN_EGG,
            IRON_GOLEM_SPAWN_EGG, LLAMA_SPAWN_EGG, MAGMA_CUBE_SPAWN_EGG,
            MOOSHROOM_SPAWN_EGG,MULE_SPAWN_EGG, OCELOT_SPAWN_EGG,
            PANDA_SPAWN_EGG, PARROT_SPAWN_EGG, PHANTOM_SPAWN_EGG,
            PIG_SPAWN_EGG, PIGLIN_SPAWN_EGG, PIGLIN_BRUTE_SPAWN_EGG,
            PILLAGER_SPAWN_EGG, POLAR_BEAR_SPAWN_EGG,
            PUFFERFISH_SPAWN_EGG, RABBIT_SPAWN_EGG, RAVAGER_SPAWN_EGG,
            SALMON_SPAWN_EGG, SHEEP_SPAWN_EGG, SHULKER_SPAWN_EGG,
            SILVERFISH_SPAWN_EGG, SKELETON_SPAWN_EGG, SKELETON_HORSE_SPAWN_EGG,
            SLIME_SPAWN_EGG, SNIFFER_SPAWN_EGG, SNOW_GOLEM_SPAWN_EGG,
            SPIDER_SPAWN_EGG, SQUID_SPAWN_EGG, STRAY_SPAWN_EGG,
            STRIDER_SPAWN_EGG, TADPOLE_SPAWN_EGG, TRADER_LLAMA_SPAWN_EGG,
            TROPICAL_FISH_SPAWN_EGG, TURTLE_SPAWN_EGG, VEX_SPAWN_EGG,
            VILLAGER_SPAWN_EGG, VINDICATOR_SPAWN_EGG, WANDERING_TRADER_SPAWN_EGG,
            WARDEN_SPAWN_EGG, WITCH_SPAWN_EGG, WITHER_SKELETON_SPAWN_EGG,
            WOLF_SPAWN_EGG, ZOGLIN_SPAWN_EGG, ZOMBIE_SPAWN_EGG,
            ZOMBIE_HORSE_SPAWN_EGG, ZOMBIE_VILLAGER_SPAWN_EGG,
            ZOMBIFIED_PIGLIN_SPAWN_EGG,
            // All buckets are illegal -- Spigot removes the item state too quickly!
            // Exception: milk buckets (we catch the item consume event).
            BUCKET, LAVA_BUCKET, WATER_BUCKET,
            AXOLOTL_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET,
            COD_BUCKET, PUFFERFISH_BUCKET, TADPOLE_BUCKET,
            LAVA_BUCKET, POWDER_SNOW_BUCKET, LEGACY_BUCKET,
            LEGACY_LAVA_BUCKET, LEGACY_WATER_BUCKET
        )

        /**
         * Validates whether the provided material is a valid type.
         * A material is considered valid if it is not air and not a block.
         *
         * @param material The material to validate.
         * @return Whether the material is valid
         */
        fun validMaterial(material: Material) =
            !material.isAir
            && material !in ILLEGAL_MATERIALS

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