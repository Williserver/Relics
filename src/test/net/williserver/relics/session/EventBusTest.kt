package net.williserver.relics.session

import net.williserver.relics.model.Relic
import net.williserver.relics.model.RelicRarity
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

/**
 * @author Willmo3
 */
class EventBusTest {
    /**
     * The test ensures that the listeners are called in the expected order:
     * - The `MODEL` listener is executed first.
     * - The `INTEGRATION` listener follows.
     *
     * The event is fired multiple times, and the final value of the sum is asserted
     * to verify correct execution and order of the listeners.
     *
     * Asserts:
     * - The final value of `sum` matches the expected outcome after all events fire.
     */
    @Test
    fun testEventsFireInOrder() {
        var sum = 1
        val bus = RelicEventBus()

        bus.registerListener(RelicEvent.REGISTER, RelicListenerType.MODEL, { _, _, _ -> sum += 2 })
        bus.registerListener(RelicEvent.REGISTER, RelicListenerType.INTEGRATION, { _, _, _ -> sum *= 2 })

        for (i in 1..25) {
            bus.fireEvent(RelicEvent.REGISTER, Relic("TestRelic", RelicRarity.Epic), UUID.randomUUID(), null)
        }
        assertEquals(167772156, sum)
    }
}