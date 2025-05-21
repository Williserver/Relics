package net.williserver.relics.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.williserver.relics.integration.item.RelicItemStackIntegrator
import net.williserver.relics.integration.item.RelicItemStackIntegrator.Companion.itemInHand
import net.williserver.relics.integration.item.RelicItemStackIntegrator.Companion.itemName
import net.williserver.relics.integration.messaging.prefixedMessage
import net.williserver.relics.integration.messaging.sendErrorMessage
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
import java.util.UUID

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
     * Executes commands related to relics based on the provided subcommand and arguments.
     *
     * @param sender The entity or console executing the command.
     * @param command The command being executed.
     * @param label The alias of the command used.
     * @param args The arguments provided along with the command.
     * @return Whether the command was invoked with the correct number of arguments.
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val subcommand = if (args.isNotEmpty()) args[0] else "help"
        // Invariant: permissions node for each subcommand.
        if (!sender.hasPermission("relics.$subcommand")) {
            sender.sendMessage(prefixedMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED)))
            return true
        }

        val execute = RelicSubcommandExecutor(sender, args.drop(1), relicSet, bus, itemIntegrator)
        return when (subcommand) {
            "claim" -> execute.claim()
            "deregister" -> execute.deregister()
            "help" -> execute.help()
            "info" -> execute.info()
            "register" -> execute.register()
            "list" -> execute.list()
            else -> false
        }
    }

    /*
     * Static helpers for interfacing with relics CLI
     */
    companion object {
        /**
         * @return the name with underscores replaced by spaces.
         */
        fun underscoresToSpaces(name: String) = name.replace("_", " ")

        /**
         * @return the name with spaces replaced by underscores.
         */
        fun spacesToUnderscores(name: String) = name.replace(" ", "_")

        /**
         * @return An item name with starting rarity string removed.
         */
        fun nameWithoutRarity(name: String) = name.replaceBefore(" ", "").trimStart()
    } // End static helpers

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
    private val itemIntegrator: RelicItemStackIntegrator
) {
    private val v = RelicsCommandValidator(s, relicSet, itemIntegrator)

    /**
     * Sends a help message to the command sender containing details about available commands.
     * @return True after successfully sending the help message.
     */
    fun help(): Boolean {
        val header = prefixedMessage(Component.text("Commands:"))
        val bullet = Component.text("\n- /relics ", NamedTextColor.GOLD)

        fun generateCommandHelp(name: String, text: String)
            = bullet.append(Component.text("$name: ", NamedTextColor.RED).append(Component.text(text, NamedTextColor.GRAY)))

        val deregister = generateCommandHelp("deregister [name]", "manually deregister a relic by name")
        val help = generateCommandHelp("help", "pull up this help menu")
        val info = generateCommandHelp("info [name]", "get information about a specific relic by name")
        val list = generateCommandHelp("list", "list all relics")
        val register = generateCommandHelp("register [name]", "register the item that you're holding as a Relic.")
        s.sendMessage(header
            .append(deregister)
            .append(help)    // Create a validator for this sender.
            .append(info)
            .append(list)
            .append(register)
        )
        return true
    }

    /**
     * Processes the claim command for a relic held by the invoking player.
     *
     * The method performs several validations to ensure that:
     * - The command sender is a valid player.
     * - The player is holding a single item.
     * - The held item is a registered relic.
     * - The relic is not already owned by the player.
     *
     * If all validations pass, the relic is claimed by the player, and a `CLAIM` event is triggered.
     *
     * @return Whether the command was invoked with the correct number of arguments.
     */
    fun claim(): Boolean {
        // Argument structure validation. One implicit argument -- relic in hand.
        if (args.isNotEmpty()) {
            return false
        }

        // Argument semantics validation.
        // Sender is a player holding a Relic they don't already own.
        if (!v.assertValidPlayer()
            || !v.assertSingleItemHeld()
            || !v.assertHeldItemIsRelic()) {
            return true
        }
        val heldItem = itemInHand(s as Player)!!
        val relic = itemIntegrator.relicFromItem(heldItem)
        val oldOwner = relicSet.ownerOf(relic)
        if (!v.assertUniqueIdIsNotSender(oldOwner, "You already own this relic!")) {
            return true
        }

        // Validation complete, claim item.
        bus.fireEvent(RelicEvent.CLAIM, relic, s.uniqueId, heldItem)
        return true
    }

    /**
     * Deregisters a relic by its name, triggering a `DESTROY` event if the relic exists.
     *
     * @return Whether the command was invoked with the correct number of arguments.
     */
    fun deregister(): Boolean {
        // Argument structure validation. One optional arg: item name.
        if (args.size > 1) {
            return false
        }

        // Argument semantics validation. Item held is a relic, or args[0] is a valid name.
        val relic = getRelicFromImplicitArgument()?: return true
        // If we have access to the item, include it in the deregister event!
        val item =
            if (args.isEmpty()) (s as Player).inventory.itemInMainHand else null
        val agent =
            if (s is Player) s.uniqueId else null

        // Validation complete, fire event.
        bus.fireEvent(RelicEvent.DESTROY, relic, agent, item)
        return true
    }

    /**
     * Retrieves and displays information about a specified relic based on the provided arguments
     * or the currently held item.
     * - Determines the relic name from the arguments or the held item's name.
     * - Error messages are sent to the sender if the validation fails.
     *
     * @return Whether the method was invoked with the correct number of arguments.
     */
    fun info(): Boolean {
        // Argument structure validation. One optional arg: item name.
        if (args.size > 1) {
            return false
        }

        // Argument semantics validation. Item held is a relic, or args[0] is a valid name.
        val relic = getRelicFromImplicitArgument() ?: return true
        // Prepare and send message.
        val message = prefixedMessage(Component.text("Relic Information:", NamedTextColor.GOLD))
            .append(relicEntry(relic))

        s.sendMessage(message)
        return true
    }

    /**
     * Sends a message to the command sender containing a formatted list of all relics.
     *
     * @return `true` after successfully sending the list message.
     */
    fun list(): Boolean {
        // Argument structure validation. One optional arg: name of player.
        if (args.size > 1) {
            return false
        }

        // Get the target player, if provided.
        val target =
            if (args.isNotEmpty()) {
                Bukkit.getOfflinePlayer(args[0]).let {
                    if (!it.hasPlayedBefore()) {
                        sendErrorMessage(s, "The player you specified has never joined the server.")
                        return true
                    } else it.uniqueId
                }
            } else null

        // Send a list of all claimed relics.
        s.sendMessage(relicSet
            .relics()
            .filter { relicSet.ownerOf(it) != null && (target == null || relicSet.ownerOf(it) == target)}
            .fold(prefixedMessage(Component.text("All Claimed Relics:", NamedTextColor.GOLD)))
                { message, relic -> message.append(relicEntry(relic)) }
        )
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
        val name = itemName(item)
        if (!v.assertValidName(name) || !v.assertUniqueName(name)) {
            return true
        }

        // Validation complete, fire event.
        bus.fireEvent(RelicEvent.REGISTER, Relic(name, RelicRarity.rarityFromName(args[0])!!), s.uniqueId, item)
        return true
    }

    // TODO: claim relic
    // -- given an item in your hand, check if it's a relic
    // -- if so, mark you as the new owner of the relic.

    // Validate:
    // -- Sender is player

    // Report:
    // -- Whether you claimed the relic or not.

    // TODO: top players
    // -- report a list of players, sorted by the value of the relics they own.

    /*
     * Internal instance-specific helpers
     */

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

    /**
     * Attempts to retrieve a relic based on the implicit context or provided arguments.
     *
     * This function determines the relic in one of the following ways:
     * - If no arguments are provided:
     *   - Verifies the sender is a player and is holding a single item.
     *   - Extracts the item's name from the player's held item and matches it against the relic set.
     * - If one argument is provided:
     *   - Converts underscores in the argument to spaces and searches for a matching relic name.
     * - If more than one argument is provided:
     *   - Returns null as no valid match can be determined.
     *
     * @return The corresponding relic if found, or null if no relic matches or input validation fails.
     */
    private fun getRelicFromImplicitArgument() =
        when (args.size) {
            0 ->
                if (!v.assertValidPlayer()
                    || !v.assertSingleItemHeld()
                    // Nominal check used to ensure that the item is still a relic.
                    || !v.assertNameRefersToRelic(nameWithoutRarity(itemName((s as Player).inventory.itemInMainHand)))
                    // Deep check used to ensure that this item has the metadata needed to guarantee relic status.
                    || !v.assertHeldItemIsRelic()) {
                    null
                } else {
                    // Use the metadata embedded in the itemStack to retrieve the relic.
                    // This further complicates counterfeiting.
                    itemIntegrator.relicFromItem(s.inventory.itemInMainHand)
                }
            1 ->
                relicSet.relicNamed(underscoresToSpaces(args[0]))
            else -> null
        }

    /**
     * @param relic Relic to format
     * @return A component mapping the relic's formatting to its owner.
     */
    private fun relicEntry(relic: Relic) =
        Component.text("\n - ", NamedTextColor.RED)
        .append(relic.asDisplayComponent())
            .append(formatOwner(relic) ?: Component.text(" (unclaimed)", NamedTextColor.GRAY))

} // end relic subcommand executor
} // end relics command