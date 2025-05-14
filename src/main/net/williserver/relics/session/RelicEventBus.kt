package net.williserver.relics.session

import net.williserver.relics.model.Relic
import java.util.UUID

/**
 * Significant events in the lifecycle of a relic, to which listeners can be registered.
 */
enum class RelicEvent {
    REGISTER,
    CLAIM,
    DESTROY
}

/**
 * Listener function registered to a specific event.
 * @param relic Relic impacted by command invocation
 * @param agent Player who caused the event.
 */
typealias RelicLifecycleListener = (relic: Relic, agent: UUID) -> Unit

/**
 * Event bus for major events in a relic's lifecycle.
 *
 * @author Willmo3
 */
class RelicEventBus {
    // TODO: register listener
    // TODO: fire event
}