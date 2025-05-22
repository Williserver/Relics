package net.williserver.relics.model

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.DARK_RED
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN

/**
 * Relics are associated with a rarity.
 * @author Willmo3
 */
@Serializable
enum class RelicRarity(val color: NamedTextColor) {
    Common(GRAY),
    Rare(GREEN),
    Epic(DARK_PURPLE),
    Legendary(GOLD),
    Unique(DARK_RED);

    /**
     * @return the number of points associated with this rarity.
     * Points expand in a Fibonnaci sequence.
     */
    fun points() = when (this) {
        Common -> 1u
        Rare -> 2u
        Epic -> 3u
        Legendary -> 5u
        Unique -> 8u
    }

    companion object {
        /**
         * Returns the corresponding `RelicRarity` enum value based on the given name. The match
         * is case-insensitive. If the name does not match any predefined rarity, `null` is returned.
         *
         * @param name The name of the rarity to be translated into a `RelicRarity` enum value.
         * @return The corresponding `RelicRarity` or `null` if no match is found.
         */
        fun rarityFromName(name: String) = when (name.lowercase()) {
            "common" -> Common
            "rare" -> Rare
            "epic" -> Epic
            "legendary" -> Legendary
            "unique" -> Unique
            else -> null
        }
    }
}