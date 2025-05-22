package net.williserver.relics.model

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

/**
 * @author Willmo3
 */
class RelicRarityTest {
    @Test
    fun testRarityPoints() {
        assertEquals(1u, RelicRarity.Common.points())
        assertEquals(2u, RelicRarity.Rare.points())
        assertEquals(3u, RelicRarity.Epic.points())
        assertEquals(5u, RelicRarity.Legendary.points())
        assertEquals(8u, RelicRarity.Unique.points())
    }
}