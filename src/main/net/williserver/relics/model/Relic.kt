package net.williserver.relics.model

import java.util.UUID

/**
 * Relics are associated with a rarity.
 */
enum class RelicRarity() {
    Common,
    Rare,
    Epic,
    Legendary,
    Unique
}

/**
 * A named relic. Contains a UUID, a rarity, and a name.
 *
 * @author Willmo3
 */
data class Relic(val id: UUID, val rarity: RelicRarity, val name: String)