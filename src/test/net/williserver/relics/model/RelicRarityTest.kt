package net.williserver.relics.model

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

/**
 * @author Willmo3
 */
class RelicRarityTest {
    @Test
    fun testRarityPoints() {
        assertEquals(1, RelicRarity.Common.points())
        assertEquals(2, RelicRarity.Rare.points())
        assertEquals(3, RelicRarity.Epic.points())
        assertEquals(5, RelicRarity.Legendary.points())
        assertEquals(8, RelicRarity.Unique.points())
    }
}