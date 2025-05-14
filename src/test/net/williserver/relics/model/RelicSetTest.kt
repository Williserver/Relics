package net.williserver.relics.model

import net.williserver.relics.LogHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Willmo3
 */
class RelicSetTest {
    /**
     * Tests the functionality of registering a relic in the `RelicSet`.
     * Verifies that a relic is not present in the relic set before registration,
     * and is correctly added after invoking the `register` method.
     *
     * Assertions:
     * - The relic is not initially part of the relic set.
     * - The relic is successfully added to the relic set after registration.
     */
    @Test
    fun testRegisterRelic() {
        val relic = Relic( "Mace of Djibuttiron", RelicRarity.Unique)
        val relicSet = RelicSet()
        assert(relic !in relicSet.relics())
        relicSet.register(relic)
        assert(relic in relicSet.relics())
    }
    
    /**
     * Verifies the functionality of writing and reading a `RelicSet` to and from a file.
     *
     * The test first creates a `RelicSet` containing two relics, claims one of them for a specific owner,
     * and writes the set to a file in JSON format. It then reads the data back from the file into another
     * `RelicSet` instance and asserts that the content of the original and the restored sets are equal.
     *
     * Key operations tested:
     * - Claiming a relic for an owner within the `RelicSet`.
     * - Writing a `RelicSet` to a file with `RelicSet.writeToFile`.
     * - Reading a `RelicSet` from a file with `RelicSet.readFromFile`.
     *
     * Assertion:
     * Ensures that the `RelicSet` written to and read from the file are equivalent.
     */
    @Test
    fun testWriteReadRelic() {
        val mace = Relic("Mace of Djibuttiron", RelicRarity.Unique)
        val sword = Relic("Sword of Rust", RelicRarity.Epic)
        val relicSet = RelicSet(mutableMapOf(Pair(mace, null), Pair(sword, null)))
        relicSet.claim(mace, java.util.UUID.randomUUID())

        RelicSet.writeToFile(LogHandler(null), "testWrite.json", relicSet)
        val relicSet2 = RelicSet.readFromFile(LogHandler(null), "testWrite.json")
        assertEquals(relicSet, relicSet2)
    }

    /**
     * Tests the functionality of claiming a relic within the `RelicSet`.
     *
     * Verifies that the owner of a relic is initially null and ensures that
     * invoking the `claim` method correctly assigns the specified owner to the relic.
     *
     * Assertions:
     * - The relic's owner is null before claiming.
     * - The relic's owner is correctly updated after claiming.
     */
    @Test
    fun testClaimRelic() {
        val mace = Relic("Mace of Djibuttiron", RelicRarity.Unique)
        val sword = Relic("Sword of Rust", RelicRarity.Epic)
        val relicSet = RelicSet(mutableMapOf(Pair(mace, null), Pair(sword, null)))
        val player = java.util.UUID.randomUUID()
        assertNull(relicSet.ownerOf(mace))
        relicSet.claim(mace, player)
        assertEquals(
            relicSet.ownerOf(mace),
            player
        )
    }
}