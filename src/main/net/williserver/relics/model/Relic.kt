package net.williserver.relics.model

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.Material

/**
 * A named relic.
 * @param name Name of relic. Should be unique in its RelicSet.
 * @param rarity Rarity of relic.
 *
 * @author Willmo3
 */
@Serializable
data class Relic(val name: String, val rarity: RelicRarity) {
    init {
        if (!validName(name)) {
            throw IllegalArgumentException("Invalid relic name: $name. Please validate with validName function.")
        }
    }

    /**
     * @return a Component representing the textual display of the relic, including its rarity and name.
     */
    fun asDisplayComponent() = Component.text("${rarity.name} $name", rarity.color)

    companion object {
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
            Material.STRUCTURE_VOID, Material.REDSTONE,
            Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
            // Arrows
            Material.LEGACY_ARROW, Material.LEGACY_SPECTRAL_ARROW, Material.LEGACY_TIPPED_ARROW,
            // All spawn eggs are illegal!
            // NOTE: creaking spawn egg does not appear supported yet...?
            Material.ALLAY_SPAWN_EGG, Material.ARMADILLO_SPAWN_EGG, Material.AXOLOTL_SPAWN_EGG,
            Material.BAT_SPAWN_EGG, Material.BEE_SPAWN_EGG, Material.BOGGED_SPAWN_EGG,
            Material.BLAZE_SPAWN_EGG, Material.BREEZE_SPAWN_EGG, Material.CAT_SPAWN_EGG,
            Material.CAVE_SPIDER_SPAWN_EGG, Material.CHICKEN_SPAWN_EGG, Material.COD_SPAWN_EGG,
            Material.COW_SPAWN_EGG, Material.CREEPER_SPAWN_EGG, Material.DOLPHIN_SPAWN_EGG,
            Material.DONKEY_SPAWN_EGG, Material.DROWNED_SPAWN_EGG, Material.ELDER_GUARDIAN_SPAWN_EGG,
            Material.ENDERMAN_SPAWN_EGG, Material.ENDERMITE_SPAWN_EGG, Material.EVOKER_SPAWN_EGG,
            Material.FOX_SPAWN_EGG, Material.FROG_SPAWN_EGG, Material.GHAST_SPAWN_EGG,
            Material.GLOW_SQUID_SPAWN_EGG, Material.GUARDIAN_SPAWN_EGG,
            Material.HOGLIN_SPAWN_EGG, Material.HORSE_SPAWN_EGG, Material.HUSK_SPAWN_EGG,
            Material.IRON_GOLEM_SPAWN_EGG, Material.LLAMA_SPAWN_EGG, Material.MAGMA_CUBE_SPAWN_EGG,
            Material.MOOSHROOM_SPAWN_EGG,Material.MULE_SPAWN_EGG, Material.OCELOT_SPAWN_EGG,
            Material.PANDA_SPAWN_EGG, Material.PARROT_SPAWN_EGG, Material.PHANTOM_SPAWN_EGG,
            Material.PIG_SPAWN_EGG, Material.PIGLIN_SPAWN_EGG, Material.PIGLIN_BRUTE_SPAWN_EGG,
            Material.PILLAGER_SPAWN_EGG, Material.POLAR_BEAR_SPAWN_EGG,
            Material.PUFFERFISH_SPAWN_EGG, Material.RABBIT_SPAWN_EGG, Material.RAVAGER_SPAWN_EGG,
            Material.SALMON_SPAWN_EGG, Material.SHEEP_SPAWN_EGG, Material.SHULKER_SPAWN_EGG,
            Material.SILVERFISH_SPAWN_EGG, Material.SKELETON_SPAWN_EGG, Material.SKELETON_HORSE_SPAWN_EGG,
            Material.SLIME_SPAWN_EGG, Material.SNIFFER_SPAWN_EGG, Material.SNOW_GOLEM_SPAWN_EGG,
            Material.SPIDER_SPAWN_EGG, Material.SQUID_SPAWN_EGG, Material.STRAY_SPAWN_EGG,
            Material.STRIDER_SPAWN_EGG, Material.TADPOLE_SPAWN_EGG, Material.TRADER_LLAMA_SPAWN_EGG,
            Material.TROPICAL_FISH_SPAWN_EGG, Material.TURTLE_SPAWN_EGG, Material.VEX_SPAWN_EGG,
            Material.VILLAGER_SPAWN_EGG, Material.VINDICATOR_SPAWN_EGG, Material.WANDERING_TRADER_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG, Material.WITCH_SPAWN_EGG, Material.WITHER_SKELETON_SPAWN_EGG,
            Material.WOLF_SPAWN_EGG, Material.ZOGLIN_SPAWN_EGG, Material.ZOMBIE_SPAWN_EGG,
            Material.ZOMBIE_HORSE_SPAWN_EGG, Material.ZOMBIE_VILLAGER_SPAWN_EGG,
            Material.ZOMBIFIED_PIGLIN_SPAWN_EGG
        )

        /**
         * Validates the name of a relic.
         * A relic must start with a non-whitespace character.
         *
         * After that, the relic name may contain any letter or digit. It may also contain apostrophes, dashes, commas, and spaces.
         */
        fun validName(name: String) =  name.matches("^([a-zA-Z0-9]|-|'|,)([a-zA-Z0-9]|-|'|,|\\s)*$".toRegex())

        /**
         * Validates whether the provided material is a valid type.
         * A material is considered valid if it is not air and not a block.
         *
         * @param material The material to validate.
         * @return Whether the material is valid
         */
        fun validMaterial(material: Material) =
            !material.isAir
            && !material.isBlock
            && material !in ILLEGAL_MATERIALS
    }
}