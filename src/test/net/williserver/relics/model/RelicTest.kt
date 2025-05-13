package net.williserver.relics.model

import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Willmo3
 */
class RelicTest {
    @Test
    fun testReadWriteRelic() {
        val relic = Relic(UUID.randomUUID(), RelicRarity.Common, "Sword of Rust")
        val relicString = Json.encodeToString(relic)
        val otherRelic = Json.decodeFromString<Relic>(relicString)
        assertEquals(relic, otherRelic)
    }
}