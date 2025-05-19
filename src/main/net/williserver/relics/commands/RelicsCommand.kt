package net.williserver.relics.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.williserver.relics.RelicsPlugin.Companion.PLUGIN_MESSAGE_PREFIX
import net.williserver.relics.integration.item.RelicItemStackIntegrator
import net.williserver.relics.model.Relic
import net.williserver.relics.model.RelicRarity
import net.williserver.relics.model.RelicSet
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * The RelicsCommand class represents a command related to relics.
 * It provides functionality to manage and execute operations associated with relic-related actions.
 *
 * @param relicSet Set of relics for this session.
 * @param bus Event bus for relic lifecycle events.
 * @param itemIntegrator Tool for mapping ItemStacks -> Relics
 * @author Willmo3
 */
class RelicsCommand(
    private val relicSet: RelicSet,
    private val bus: RelicEventBus,
    private val itemIntegrator: RelicItemStackIntegrator): CommandExecutor {

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
            val execute = RelicSubcommandExecutor(sender, args.drop(1), relicSet, bus, itemIntegrator)

            when (subcommand) {
                "help" -> execute.help()
                "register" -> execute.register()
                "list" -> execute.list()
                else -> false
            }
        } else RelicSubcommandExecutor(sender, args.toList(), relicSet, bus, itemIntegrator).help()

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
 * @param bus Event bus for this session.
 * @param itemIntegrator Tool for mapping itemStacks -> relics
 */
private class RelicSubcommandExecutor(
    private val s: CommandSender,
    private val args: List<String>,
    private val relicSet: RelicSet,
    private val bus: RelicEventBus,
    itemIntegrator: RelicItemStackIntegrator
) {
    // Create a validator for this sender.
    private val v = RelicsCommandValidator(s, itemIntegrator)

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
        val register = generateCommandHelp("register [name]", "register the item that you're holding as a Relic.")
        s.sendMessage(header.append(help).append(register))
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
     * @return Whether the command was invoked with the correct number of arguments.
     */
    fun register(): Boolean {
        // Argument structure validation. One arg: item rarity.
        if (args.size != 1) {
            return false
        }

        // Argument semantics validation.
        if (!v.assertValidPlayer()
            || !v.assertSingleItemHeld()
            || !v.assertHeldItemValidMaterial()
            || !v.assertHeldItemNotAlreadyRelic()
            || !v.assertRarityValid(args[0])) {
            return true
        }

        val item = (s as Player).inventory.itemInMainHand
        val name =
            if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
                // Since we're registering relic under plaintext display name, acceptable to use this.
                item.itemMeta!!.displayName
            } else {
                item.type.name.lowercase().replace('_', ' ')
            }

        if (!v.assertValidName(name) || !v.assertUniqueName(name, relicSet)) {
            return true
        }

        // Fire event, informing listeners to perform operation.
        bus.fireEvent(RelicEvent.REGISTER, Relic(name, RelicRarity.rarityFromName(args[0])!!), s.uniqueId, item)
        sendCongratsMessage(s, "Registered a new relic named \"$name\".")
        return true
    }

    /**
     * Sends a message to the command sender containing a formatted list of all relics.
     *
     * @return `true` after successfully sending the list message.
     */
    fun list(): Boolean {
        var message = Component.text("$PLUGIN_MESSAGE_PREFIX All Relics: ", NamedTextColor.GOLD)

        fun relicEntry(relic: Relic)
            = Component.text("\n - ", NamedTextColor.RED)
            .append(relic.asDisplayComponent())
            .append(formatOwner(relic) ?: Component.text(" (unclaimed)", NamedTextColor.GRAY))

        relicSet.relics().forEach { message = message.append(relicEntry(it)) }
        s.sendMessage(message)
        return true
    }

    /**
     * Formats the ownership information of a relic if the relic has an associated owner.
     *
     * @param relic The relic for which ownership information is to be formatted. Must be a registered relic.
     * @return A `TextComponent` containing the ownership information, or null if the relic has no owner.
     */
    private fun formatOwner(relic: Relic): TextComponent? =
        relicSet.ownerOf(relic)?.let {
            val name = Bukkit.getOfflinePlayer(it).name ?: it.toString()

            Component.text(" (owned by ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text(")", NamedTextColor.GRAY))
        }

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

    // TODO: top players
    // -- report a list of players, sorted by the value of the relics they own.

    // TODO: relic by player.
    // -- given a player, report what relics they own.

} // end relic subcommand executor
} // end relics command