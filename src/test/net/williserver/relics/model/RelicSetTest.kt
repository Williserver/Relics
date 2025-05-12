package net.williserver.relics.model

import java.util.UUID
import kotlin.test.Test

/**
 * @author Willmo3
 */
class RelicSetTest {
    @Test
    fun testRegisterRelic() {
        val relic = Relic(UUID.randomUUID(), RelicRarity.Unique, "Mace of Djibuttiron")
        val relicSet = RelicSet()
        assert(relic !in relicSet.relics())
        relicSet.register(relic)
        assert(relic in relicSet.relics())
    }
}