package net.williserver.relics.model

import net.williserver.relics.LogHandler
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Willmo3
 */
class RelicSetTest {
    @Test
    fun testRegisterRelic() {
        val relic = Relic( "Mace of Djibuttiron", RelicRarity.Unique)
        val relicSet = RelicSet()
        assert(relic !in relicSet.relics())
        relicSet.register(relic)
        assert(relic in relicSet.relics())
    }
    
    @Test
    fun testWriteReadRelic() {
        val mace = Relic("Mace of Djibuttiron", RelicRarity.Unique)
        val sword = Relic("Sword of Rust", RelicRarity.Epic)
        val relicSet = RelicSet(mutableMapOf(Pair(mace, null), Pair(sword, null)))

        RelicSet.writeToFile(LogHandler(null), "testWrite.json", relicSet)
        val relicSet2 = RelicSet.readFromFile(LogHandler(null), "testWrite.json")
        assertEquals(relicSet, relicSet2)
    }
}