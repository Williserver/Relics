package net.williserver.relics.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.williserver.relics.model.RelicSet
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * The RelicsCommand class represents a command related to relics.
 * It provides functionality to manage and execute operations associated with relic-related actions.
 *
 * @param relicSet Set of relics for this session.
 * @author Willmo3
 */
class RelicsCommand(private val relicSet: RelicSet): CommandExecutor {

    /**
     * Handles the execution of a command related to relics.
     *
     * This method processes the command sent by the specified sender
     * and determines the appropriate action to take based on the provided arguments.
     * If no arguments are provided, it defaults to sending a help message to the sender.
     *
     * @param sender Source of the command, such as a player or the console.
     * @param command Command being executed.
     * @param label Alias used for the command.
     * @param args Arguments for subcommand
     * @return Whether the command was invoked with the correct number of arguments.
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>) =
        if (args.isNotEmpty()) {
            val subcommand = args[0]
            val execute = RelicSubcommandExecutor(sender, args.drop(1), relicSet)

            when (subcommand) {
                "help" -> execute.help()
                "register" -> execute.register()
                else -> false
            }
        } else RelicSubcommandExecutor(sender, args.toList(), relicSet).help()

/**
 * Handles the execution of subcommands related to relics.
 *
 * This class processes and manages the input from a command sender, such as a player or console,
 * along with any accompanying arguments specific to a relic subcommand. It also interfaces with
 * the provided relic set to perform operations related to relic management or retrieval.
 *
 * @property s sender of the command, such as a player or console.
 * @property args list of arguments provided with the subcommand.
 * @property relicSet set of relics for the subcommand.
 */
private class RelicSubcommandExecutor(
    private val s: CommandSender,
    private val args: List<String>,
    private val relicSet: RelicSet
) {
    // Create a validator for this sender.
    private val v = RelicsCommandValidator(s)

    /**
     * Sends a help message to the command sender containing details about available commands.
     * @return True after successfully sending the help message.
     */
    fun help(): Boolean {
        val header = prefixedMessage(Component.text("Commands:"))
        val bullet = Component.text("\n- /relics ", NamedTextColor.GOLD)

        fun generateCommandHelp(name: String, text: String)
            = bullet.append(Component.text("$name: ", NamedTextColor.RED).append(Component.text(text, NamedTextColor.GRAY)))

        val help = generateCommandHelp("help", "pull up this help menu")
        s.sendMessage(header.append(help))
        return true
    }


    /**
     * Validates the command sender, ensures they are holding a single item, and checks if the item's name is valid.
     *
     * This method performs the following checks in order:
     * - Ensures the sender is a valid player.
     * - Confirms the player is holding a single item in their hand.
     * - Validates the display name of the item in the player's main hand.
     *
     * Each validation step sends an error message to the sender if the check fails.
     *
     * @return Whether all validations pass.
     */
    fun register(): Boolean {
        if (!v.assertValidPlayer()
            || !v.assertSingleItemHeld()) {
            return true
        }

        val name = (s as Player).inventory.itemInMainHand.displayName().examinableName()
        if (!v.assertValidName(name) || !v.assertUniqueName(name, relicSet)) {
            return true
        }

        return true
    }

    // TODO: register relic
    // -- given an item in your hand, add its relic name as an NBT tag.
    // -- add its name to the relic list.

    // Validate:
    // -- Sender is player
    // -- Exactly one item in hand. (i.e. itemstack size 1)
    // -- Name is valid -- i.e. nonempty, nothing dangerous for serialization
    // TODO: VALIDNAME

    // Remove the item from their inventory
    // Add a copy of the item with added metadata
    // Place in the list

    // TODO: relic info
    // -- given an item in your hand, check if it is a relic.
    // -- if so, report information.

    // Validate:
    // -- Sender is player

    // Return
    // -- Information about the item, or nothing if no info.

    // TODO: claim relic
    // -- given an item in your hand, check if it's a relic
    // -- if so, mark you as the new owner of the relic.

    // Validate:
    // -- Sender is player

    // Report:
    // -- Whether you claimed the relic or not.

    // TODO: list relics
    // -- report a list of all relics on the server to sender.

    // TODO: top players
    // -- report a list of players, sorted by the value of the relics they own.

    // TODO: relic by player.
    // -- given a player, report what relics they own.

} // end relic subcommand executor
} // end relics command