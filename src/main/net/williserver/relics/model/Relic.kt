package net.williserver.relics.model

import kotlinx.serialization.Serializable

/**
 * Relics are associated with a rarity.
 */
@Serializable
enum class RelicRarity() {
    Common,
    Rare,
    Epic,
    Legendary,
    Unique
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