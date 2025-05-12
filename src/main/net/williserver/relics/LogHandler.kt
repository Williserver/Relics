package net.williserver.relics

import java.util.logging.Logger

/**
 * Utility wrapper to allow logging outside Bukkit server environments.
 * @author Willmo3
 */
class LogHandler(private val logger: Logger?) {
    /**
     * Report an error to server console or to stderr if no console present.
     * @param message Message to report.
     */
    fun err(message: String) {
        logger?.warning(message) ?: System.err.println("${RelicsPlugin.PLUGIN_MESSAGE_PREFIX}: $message")
    }

    /**
     * Report a message to server console or to stdout if no console present.
     * @param message Message to report.
     */
    fun info(message: String) {
        logger?.info(message) ?: println("${RelicsPlugin.PLUGIN_MESSAGE_PREFIX}: $message")
    }
}