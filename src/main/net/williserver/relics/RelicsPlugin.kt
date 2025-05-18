package net.williserver.relics

import net.williserver.relics.commands.RelicsCommand
import net.williserver.relics.commands.RelicsTabCompleter
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
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
        saveDefaultConfig() // Ensure data directory prepared, even if no options present.

        /* Initialize relic model */
        relicSet = RelicSet.readFromFile(logger, path)
        logger.info("Finished initializing relic set.")

        /* Register relic lifecycle listeners. */
        val eventBus = RelicEventBus()
        eventBus.registerListener(RelicEvent.REGISTER, relicSet.constructRegisterListener())
        logger.info("Finished registering relic lifecycle listeners.")

        /* Register commands.*/
        getCommand("relics")!!.setExecutor(RelicsCommand(relicSet, eventBus))
        getCommand("relics")!!.tabCompleter = RelicsTabCompleter()
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