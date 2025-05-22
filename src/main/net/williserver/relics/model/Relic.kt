package net.williserver.relics.model

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Material.*

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
            BARRIER, STRUCTURE_VOID,
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
            && material !in ILLEGAL_MATERIALS
    }
}