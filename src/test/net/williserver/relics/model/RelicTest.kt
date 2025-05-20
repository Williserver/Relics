package net.williserver.relics.model

import org.junit.jupiter.api.Assertions.assertThrows
import java.lang.IllegalArgumentException
import kotlin.test.Test

/**
 * @author Willmo3
 */
class RelicTest {
    /**
     * Tests the behavior of the `Relic` class when the name parameter is empty.
     *
     * Specifically, verifies the constructor of `Relic` does not throw an exception
     * for a valid non-empty name and that it throws an `IllegalArgumentException`
     * when the name is empty.
     */
    @Test
    fun testRelicName() {
        Relic("T's bottle-o-wine", RelicRarity.Rare)
        assertThrows(IllegalArgumentException::class.java) { Relic("", RelicRarity.Rare) }
        assertThrows(IllegalArgumentException::class.java) { Relic(" Leading whitespace!", RelicRarity.Rare) }
    }
}