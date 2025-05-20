package net.williserver.relics.integration.messaging

import net.williserver.relics.session.RelicLifecycleListener

/**
 * @return A listener function that announces when a new relic is registered.
 */
fun constructRelicRegisterMessageListener(): RelicLifecycleListener
    = { relic, _, _ -> broadcastPrefixedMessage("A new ${relic.rarity} relic named \"${relic.name}\" has been forged!") }

/**
 * @return A listener function to handle relic destruction events by broadcasting a formatted message.
 */
fun constructRelicDestroyMessageListener(): RelicLifecycleListener
    = { relic, _, _ -> broadcastPrefixedMessage("The relic \"${relic.name}\" has been DESTROYED!") }