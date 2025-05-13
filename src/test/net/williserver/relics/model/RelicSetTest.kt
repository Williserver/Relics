package net.williserver.relics.model

import net.williserver.relics.LogHandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

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
    
    @Test
    fun testWriteReadRelic() {
        val mace = Relic(UUID.randomUUID(), RelicRarity.Unique, "Mace of Djibuttiron")
        val sword = Relic(UUID.randomUUID(), RelicRarity.Epic, "Sword of Rust")
        val relicSet = RelicSet(mutableSetOf(mace, sword))

        RelicSet.writeToFile(LogHandler(null), "testWrite.json", relicSet)
        val relicSet2 = RelicSet.readFromFile(LogHandler(null), "testWrite.json")
        assertEquals(relicSet, relicSet2)
    }
}