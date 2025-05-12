package net.williserver.relics

import org.bukkit.plugin.java.JavaPlugin

/**
 * RelicsPlugin, a plugin for tracking special items.
 *
 * @author Willmo3
 */
class RelicsPlugin: JavaPlugin() {
    private val logger = LogHandler(super.logger)

    override fun onEnable() {
        logger.info("Relics plugin enabled!")
    }

    override fun onDisable() {
        logger.info("Relics plugin disabled!")
    }

    companion object {
        const val PLUGIN_MESSAGE_PREFIX = "[RELICS]"
    }
}