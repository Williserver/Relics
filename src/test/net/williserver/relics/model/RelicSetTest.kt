package net.williserver.relics.model

import net.williserver.relics.LogHandler
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicListenerType
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.UUID
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
        assert(relic !in relicSet)
        relicSet.register(relic)
        assert(relic in relicSet)
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
        relicSet.claim(mace, UUID.randomUUID())

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
        val relicSet = RelicSet(mutableMapOf(Pair(mace, null)))
        val player = UUID.randomUUID()

        assertNull(relicSet.ownerOf(mace))
        relicSet.claim(mace, player)
        assertEquals(
            relicSet.ownerOf(mace),
            player
        )

        // attempting to view the owner of an unclaimed relic results in an exception.
        assertThrows(IllegalArgumentException::class.java) { relicSet.ownerOf(sword) }
    }

    /**
     * Tests the destruction functionality of a relic within the `RelicSet`.
     *
     * This test ensures that a relic can be successfully removed from the `RelicSet`.
     * It begins by creating a `Relic` and adding it to the `RelicSet`. Then, the `destroy` method
     * is invoked on the relic, and it is verified that the relic is no longer a part of the `RelicSet`.
     *
     * Assertions:
     * - The relic exists in the `RelicSet` before destruction.
     * - The relic does not exist in the `RelicSet` after destruction.
     */
    @Test
    fun testDestroyRelic() {
        val relic = Relic("Sword of Damocles", RelicRarity.Legendary)
        val relicSet = RelicSet(mutableMapOf(Pair(relic, null)))
        assert(relic in relicSet)
        relicSet.destroy(relic)
        assert(relic !in relicSet)

        assertThrows(IllegalArgumentException::class.java) { relicSet.destroy(relic) }
    }

    /**
     * Tests the functionality of the `relicNamed` method in the `RelicSet` class.
     *
     * Ensures that a relic can be retrieved by its name when it is part of the `RelicSet`.
     *
     * Assertions:
     * - Confirms that the retrieved relic matches the one registered in the `RelicSet`.
     */
    @Test
    fun testRelicFromName() {
        val relic = Relic("Mace of Djibuttiron", RelicRarity.Unique)
        val relicSet = RelicSet(mutableMapOf(Pair(relic, null)))
        assertEquals(relicSet.relicNamed("Mace of Djibuttiron"), relic)
    }

    /**
     * Tests the functionality of the relic registration listener within the `RelicEventBus`.
     *
     * Verifies that when the `REGISTER` event is fired for a relic, the appropriate listener
     * defined in the `RelicSet` correctly handles the event by registering the relic.
     *
     * Assertions:
     * - Confirms that the relic is successfully added to the relic set after the event is fired.
     */
    @Test
    fun testRelicRegisterListener() {
        val relic = Relic("test", RelicRarity.Common)
        val relicSet = RelicSet()

        val bus = RelicEventBus()
        bus.registerListener(RelicEvent.REGISTER, RelicListenerType.MODEL,relicSet.constructRegisterListener())
        bus.fireEvent(RelicEvent.REGISTER, relic, UUID.randomUUID())

        assert(relic in relicSet)
    }

    /**
     * Tests the claim listener functionality within the `RelicEventBus`.
     *
     * This test verifies that when the `CLAIM` event is fired for a relic,
     * the listener created by `RelicSet.constructClaimListener` correctly assigns
     * an owner to the relic within the `RelicSet`.
     *
     * Assertions:
     * - Confirms that the owner of the relic is correctly updated to the claimant
     *   after the `CLAIM` event is processed.
     */
    @Test
    fun testRelicClaimListener() {
        val relic = Relic("test", RelicRarity.Common)
        val relicSet = RelicSet()
        relicSet.register(relic)

        val bus = RelicEventBus()
        bus.registerListener(RelicEvent.CLAIM, RelicListenerType.MODEL,relicSet.constructClaimListener())

        val claimant = UUID.randomUUID()

        bus.fireEvent(RelicEvent.CLAIM, relic, claimant)
        assert(relicSet.ownerOf(relic) == claimant)
    }

    /**
     * Tests the functionality of the relic destruction listener within the `RelicEventBus`.
     *
     * Verifies that when the `DESTROY` event is fired for a relic, the listener created by
     * `RelicSet.constructDestroyListener` appropriately removes the relic from the `RelicSet`.
     *
     * Assertions:
     * - Confirms the relic is part of the relic set before the `DESTROY` event is fired.
     * - Confirms the relic is no longer part of the relic set after the event is processed.
     * - Ensures that attempting to fire the `DESTROY` event for an already removed relic
     *   results in an `IllegalArgumentException`.
     */
    @Test
    fun testRelicDestroyListener() {
        val relic = Relic("test", RelicRarity.Common)
        val relicSet = RelicSet()
        relicSet.register(relic)

        val bus = RelicEventBus()
        bus.registerListener(RelicEvent.DESTROY, RelicListenerType.MODEL, relicSet.constructDestroyListener())

        assert(relic in relicSet)

        bus.fireEvent(RelicEvent.DESTROY, relic, UUID.randomUUID())
        assert(relic !in relicSet)

        assertThrows(IllegalArgumentException::class.java) { bus.fireEvent(RelicEvent.DESTROY, relic, UUID.randomUUID()) }
    }
}