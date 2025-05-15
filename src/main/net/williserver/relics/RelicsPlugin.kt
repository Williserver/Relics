package net.williserver.relics

import net.williserver.relics.commands.RelicsCommand
import net.williserver.relics.commands.RelicsTabCompleter
import net.williserver.relics.model.RelicSet
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

        getCommand("relics")!!.setExecutor(RelicsCommand(RelicSet()))
        getCommand("relics")!!.tabCompleter = RelicsTabCompleter()
    }

    override fun onDisable() {
        logger.info("Relics plugin disabled!")
    }

    companion object {
        const val PLUGIN_MESSAGE_PREFIX = "[RELICS]"
    }
}