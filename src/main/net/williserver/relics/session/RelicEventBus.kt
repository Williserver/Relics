package net.williserver.relics.session

import net.williserver.relics.model.Relic
import org.bukkit.inventory.ItemStack
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
 * @param item Item stack associated with the event, if applicable.
 */
typealias RelicLifecycleListener = (relic: Relic, agent: UUID, item: ItemStack?) -> Unit

/**
 * Event bus for major events in a relic's lifecycle.
 *
 * @author Willmo3
 */
class RelicEventBus {
    private val listeners = mutableMapOf<RelicEvent, MutableSet<RelicLifecycleListener>>()

    init {
        RelicEvent.entries.forEach { listeners[it] = mutableSetOf() }
    }

    /**
     * Registers a listener for a specific relic lifecycle event.
     *
     * @param event The relic lifecycle event for which the listener is to be registered.
     * @param listener The listener to be invoked when the specified event occurs.
     */
    fun registerListener(event: RelicEvent, listener: RelicLifecycleListener) = listeners[event]!!.add(listener)

    /**
     * Triggers the specified relic event, notifying all registered listeners for that event.
     *
     * @param event The relic lifecycle event to be triggered.
     * @param relic The relic associated with the event.
     * @param agent The UUID of the agent or entity responsible for the event.
     * @param item The item stack associated with the event, if applicable.
     */
    fun fireEvent(event: RelicEvent, relic: Relic, agent: UUID, item: ItemStack? = null) = listeners[event]!!.forEach { it(relic, agent, item) }
}