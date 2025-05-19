package net.williserver.relics.integration.messaging

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.williserver.relics.RelicsPlugin
import org.bukkit.Bukkit.broadcast
import org.bukkit.command.CommandSender

/**
 * Send a red-colored error message to a target.
 *
 * @param target Entity to receive error.
 * @param message Error to format and send to target.
 */
fun sendErrorMessage(target: CommandSender, message: String)
    = target.sendMessage(prefixedMessage(Component.text(message, NamedTextColor.RED)))

/**
 * Send a green colored congratulatory message to a target.
 *
 * @param target Entity to receive message.
 * @param message Message to format and send to target.
 */
fun sendCongratsMessage(target: CommandSender, message: String)
    = target.sendMessage(prefixedMessage(Component.text(message, NamedTextColor.GREEN)))

/**
 * Append a message prefix component onto a message component.
 *
 * @param message Message to append the plugin prefix to.
 * @return A new component with the plugin prefix appended.
 */
fun prefixedMessage(message: Component)
    = Component.text("${RelicsPlugin.PLUGIN_MESSAGE_PREFIX}: ", NamedTextColor.GOLD).append(message)

/**
 * Broadcast a string message. Color will be purple.
 *
 * @param message Message to format and broadcast.
 */
fun broadcastPrefixedMessage(message: String)
        = broadcast(prefixedMessage(Component.text(message, NamedTextColor.DARK_PURPLE)))