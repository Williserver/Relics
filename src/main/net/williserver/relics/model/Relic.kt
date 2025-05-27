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
         * Validates the name of a relic.
         * A relic must start with a non-whitespace character.
         *
         * After that, the relic name may contain any letter or digit. It may also contain apostrophes, dashes, commas, and spaces.
         */
        fun validName(name: String) =  name.matches("^([a-zA-Z0-9]|-|'|,)([a-zA-Z0-9]|-|'|,|\\s)*$".toRegex())
    }
}