package net.williserver.relics.model

import net.williserver.relics.RelicsPlugin.Companion.PLUGIN_MESSAGE_PREFIX

/**
 * Set of tracked relics and their owners, if they have one.
 *
 * @author Willmo3
 */
class RelicSet {
    private val allRelics = mutableSetOf<Relic>()
//    private val relicToOwner = mutableMapOf<Relic, UUID>() TODO: need mapping of relic to owner.

    /**
     * @param relic new relic to register in this set.
     * @throws IllegalArgumentException if this relic is identical to another which has already been registered.
     */
    fun register(relic: Relic) =
        if (relic in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has already been registered!")
        } else {
            allRelics += relic
        }

    /**
     * @return an immutable set view of all relics tracked by this plugin.
     */
    fun relics() = allRelics.toSet()
}