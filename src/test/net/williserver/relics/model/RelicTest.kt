package net.williserver.relics.model

import org.junit.jupiter.api.Assertions.assertThrows
import java.lang.IllegalArgumentException
import kotlin.test.Test

class RelicTest {
    @Test
    fun testRelicEmptyName() {
        Relic("T", RelicRarity.Rare)
        assertThrows(IllegalArgumentException::class.java) { Relic("", RelicRarity.Rare) }
    }
}