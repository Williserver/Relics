package net.williserver.relics

import net.williserver.relics.commands.RelicsCommand
import net.williserver.relics.commands.RelicsTabCompleter
import net.williserver.relics.integration.item.RelicItemStackIntegrator
import net.williserver.relics.integration.serverevents.ServerRelicItemStackDestroyListener
import net.williserver.relics.integration.messaging.constructRelicDestroyMessageListener
import net.williserver.relics.integration.messaging.constructRelicRegisterMessageListener
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import net.williserver.relics.session.RelicListenerType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * RelicsPlugin, a plugin for tracking special items.
 *
 * @author Willmo3
 */
class RelicsPlugin: JavaPlugin() {
    private val logger = LogHandler(super.logger)
    private val path = "$dataFolder${File.separator}relics.json"
    // Relic data model
    private lateinit var relicSet: RelicSet

    override fun onEnable() {
        /* Plugin compatibility warnings */
        if (server.pluginManager.getPlugin("VeinMiner") != null) {
            logger.err("VeinMiner detected. Ensure that RepairFriendly option is set to true to avoid exploits!")
        }
        if (server.pluginManager.getPlugin("Timber") != null) {
            logger.err("Timber detected. Be advised that Timber treecutting can result in Relics becoming untrackable!")
        }

        // Ensure data directory prepared, even if no options present.
        saveDefaultConfig()

        /* Initialize relic model */
        relicSet = RelicSet.readFromFile(logger, path)
        logger.info("Finished initializing relic set.")

        /* Register relic lifecycle listeners. */
        val eventBus = RelicEventBus()

        /* Core model listeners. */
        eventBus.registerListener(RelicEvent.REGISTER, RelicListenerType.MODEL, relicSet.constructRegisterListener())
        eventBus.registerListener(RelicEvent.DESTROY, RelicListenerType.MODEL, relicSet.constructDestroyListener())
        eventBus.registerListener(RelicEvent.CLAIM, RelicListenerType.MODEL, relicSet.constructClaimListener())

        /* Integration listeners. */
        val integrator = RelicItemStackIntegrator(this, relicSet, eventBus)
        eventBus.registerListener(RelicEvent.REGISTER, RelicListenerType.INTEGRATION, integrator.constructRegisterItemStackListener())
        eventBus.registerListener(RelicEvent.DESTROY, RelicListenerType.INTEGRATION, integrator.constructDestroyItemStackListener())

        /* Messaging listeners. */
        eventBus.registerListener(RelicEvent.REGISTER, RelicListenerType.MESSAGING, constructRelicRegisterMessageListener())
        eventBus.registerListener(RelicEvent.DESTROY, RelicListenerType.MESSAGING, constructRelicDestroyMessageListener())
        logger.info("Finished registering relic lifecycle listeners.")

        /* Register in-game event listeners. */
        server.pluginManager.registerEvents(ServerRelicItemStackDestroyListener(integrator), this)

        /* Register commands.*/
        getCommand("relics")!!.setExecutor(RelicsCommand(relicSet, eventBus, integrator))
        getCommand("relics")!!.tabCompleter = RelicsTabCompleter(relicSet)
        logger.info("Finished registering commands.")

        logger.info("Relics plugin enabled.")
    }

    override fun onDisable() {
        RelicSet.writeToFile(logger, path, relicSet)
        logger.info("Finished writing relic set to file: $path.")

        logger.info("Relics plugin disabled.")
    }

    companion object {
        const val PLUGIN_MESSAGE_PREFIX = "[RELICS]"
    }
}