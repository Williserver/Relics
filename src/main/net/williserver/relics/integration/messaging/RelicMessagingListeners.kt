package net.williserver.relics.integration.messaging

import net.williserver.relics.session.RelicLifecycleListener

// TODO: add register listener -- state "relic forged"
// -- Hints at future direction!

/**
 * @return A listener function to handle relic destruction events by broadcasting a formatted message.
 */
fun constructRelicDestroyMessageListener(): RelicLifecycleListener = { relic, _, _ -> broadcastPrefixedMessage("The relic \"${relic.name}\" has been DESTROYED!") }