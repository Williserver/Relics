package net.williserver.relics.model

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.*

/**
 * Relics are associated with a rarity.
 */
@Serializable
enum class RelicRarity(val color: NamedTextColor) {
    Common(GRAY),
    Rare(GREEN),
    Epic(DARK_PURPLE),
    Legendary(GOLD),
    Unique(DARK_RED);

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
         * Validates the name of a relic.
         * A relic must start with a non-whitespace character.
         *
         * After that, the relic name may contain any letter or digit. It may also contain apostrophes, underscores, dashes, and spaces.
         */
        fun validName(name: String) =  name.matches("^([a-zA-Z0-9]|-|_|')([a-zA-Z0-9]|-|_|'|\\s)*$".toRegex())
    }
}