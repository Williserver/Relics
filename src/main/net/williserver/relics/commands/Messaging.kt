package net.williserver.relics.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.williserver.relics.RelicsPlugin
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
 * Append a message prefix component onto a message component.
 *
 * @param message Message to append the plugin prefix to.
 * @return A new component with the plugin prefix appended.
 */
fun prefixedMessage(message: Component)
        = Component.text("${RelicsPlugin.PLUGIN_MESSAGE_PREFIX}: ", NamedTextColor.GOLD).append(message)