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
import net.williserver.relics.model.RelicSet.Companion.sortRelics
import net.williserver.relics.session.RelicEvent
import net.williserver.relics.session.RelicEventBus
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.collections.sortedWith

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
            "all" -> execute.all()
            "claim" -> execute.claim()
            "deregister" -> execute.deregister()
            "help" -> execute.help()
            "info" -> execute.info()
            "register" -> execute.register()
            "list" -> execute.list()
            "top" -> execute.top()
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
         * Number of relics to be displayed on a single page of a paged command.
         */
        const val ENTRIES_PER_PAGE = 10u

        /**
         * @param T type of collection being page-ified.
         * @param entries Collection of paged entries to see.
         * @param entriesPerPage How many entries of the collection should be on a single page.
         * @return The subset of the collection corresponding to the selected page.
         */
        fun<T> contentsOfPage(entries: Collection<T>, entriesPerPage: UInt, selectedPage: UInt) =
            entries.drop(selectedPage.toInt() * entriesPerPage.toInt()).take(entriesPerPage.toInt())
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

        val help = generateCommandHelp("help", "pull up this help menu")
        val info = generateCommandHelp("info [name]", "get information about a specific relic by name")
        val list = generateCommandHelp("list (ownername) (page no)", "list all relics")
        val top = generateCommandHelp("top (page no)", "list a ranking of players by the number of relic points they have.")

        s.sendMessage(header
            .append(help)
            .append(info)
            .append(list)
            .append(top)
        )
        return true
    }

    /**
     * Report a list of all relics on the server to sender, whether they're owned or not.
     */
    fun all(): Boolean {
        // Argument structure validation. One optional arg: page to use (default for none: 0)
        if (args.size > 1) {
            return false
        }

        // TODO: move to object or function
        val sortedRelics = sortRelics(relicSet.relics())
        val numRelics = sortedRelics.size.toUInt()

        var lastPageNumber = numRelics / ENTRIES_PER_PAGE
        // Edge case: a multiple of RELICS_PER_PAGE relics -- last page is previous.
        if (lastPageNumber > 0u && numRelics % ENTRIES_PER_PAGE == 0u) {
            lastPageNumber--
        }

        /*
         * Selected page:
         * - 0 if no / invalid args.
         * - lastPageNumber if out of range.
         * - Otherwise, args[0] value.
         */
        val selectedPage =
            if (args.isNotEmpty()) {
                val potentialPage = args[0].toUIntOrNull() ?: 0u
                if (potentialPage > lastPageNumber) {
                    lastPageNumber
                } else potentialPage
            } else 0u

        s.sendMessage(
            contentsOfPage(sortedRelics, ENTRIES_PER_PAGE, selectedPage)
            .fold(prefixedMessage(Component.text("All Relics:", NamedTextColor.RED)))
            { message, relic -> message.append(formatRelicEntry(relic)) }
            .append(Component.text("\n\nPage $selectedPage of $lastPageNumber", NamedTextColor.GRAY))
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
        val message = prefixedMessage(Component.text("Relic Information:", NamedTextColor.RED))
            .append(formatRelicEntry(relic))

        s.sendMessage(message)
        return true
    }

    /**
     * Sends a formatted list of claimed relics to the sender. Optionally filters
     * by the specified player's relics if a name is provided as an argument.
     *
     * @return Whether the command was invoked with the correct number of arguments.
     */
    fun list(): Boolean {
        // Argument structure validation. Two optional args: (name of player, page no)
        if (args.size > 2) {
            return false
        }

        // Parse args.
        var ownerId: UUID? = null
        var selectedPage = 0u

        // If two args, playerName is first, page is second.
        if (args.size == 2) {
            ownerId = getPlayerFromName(args[0])
            if (ownerId == null) {
                sendErrorMessage(s, "Player \"${args[0]}\" not found.")
                return true
            }
            args[1].toUIntOrNull().let {
                if (it == null) {
                    sendErrorMessage(s, "Invalid page number: ${args[1]}")
                    return true
                } else {
                    selectedPage = it
                }
            }
        // If one arg, either a player name or a page number.
        // Keep in mind: a player name may be only numbers -- they will have to use two args.
        } else if (args.size == 1) {
            args[0].toUIntOrNull().let {
                if (it != null) {
                    selectedPage = it
                } else {
                    ownerId = getPlayerFromName(args[0])
                    if (ownerId == null) {
                        sendErrorMessage(s, "Player \"${args[0]}\" not found.")
                        return true
                    }
                }
            }
        }

        // Get set of relics to consider.
        val baseRelics =
            if (ownerId != null) {
                relicSet.ownedRelics().filter { relicSet.ownerOf(it) == ownerId }
            } else relicSet.ownedRelics()
        val sortedRelics = sortRelics(baseRelics)

        /*
         * Selected page:
         * - 0 if no / invalid args.
         * - lastPageNumber if out of range.
         * - Otherwise, args[0] value.
         */
        val numRelics = sortedRelics.size.toUInt()
        var lastPageNumber = numRelics / ENTRIES_PER_PAGE
        // Edge case: a multiple of RELICS_PER_PAGE relics -- last page is previous.
        if (lastPageNumber > 0u && numRelics % ENTRIES_PER_PAGE == 0u) {
            lastPageNumber--
        }
        // If user selects out of bounds page, go back to OG.
        if (selectedPage > lastPageNumber) {
            selectedPage = lastPageNumber
        }

        // Send a list of all claimed relics.
        s.sendMessage(
            contentsOfPage(sortedRelics, ENTRIES_PER_PAGE, selectedPage)
                .fold(prefixedMessage(Component.text("Claimed Relics:", NamedTextColor.RED)))
                { message, relic -> message.append(formatRelicEntry(relic)) }
                .append(Component.text("\n\nPage $selectedPage of $lastPageNumber", NamedTextColor.GRAY))
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

    /**
     * Send a message of the top players by relic points.
     * @return true after successfully sending the message.
     */
    fun top(): Boolean {
        // Argument structure validation. One optional argument: page no.
        if (args.size > 1) {
            return false
        }

        val basePlayerList = relicSet
            .playersToRelicPoints()
                .map { (Bukkit.getOfflinePlayer(it.key).name ?: "Unknown") to it.value }
                .sortedWith(compareByDescending
                { playerToOwned: Pair<String, UInt> -> playerToOwned.second }.thenBy
                { playerToOwned: Pair<String, UInt> -> playerToOwned.first })

        /*
         * Selected page:
         * - 0 if no / invalid args.
         * - lastPageNumber if out of range.
         * - Otherwise, args[0] value.
         */
        val numPlayers = basePlayerList.size.toUInt()
        var lastPageNumber = numPlayers / ENTRIES_PER_PAGE
        // Edge case: a multiple of ENTRIES_PER_PAGE players -- last page is previous.
        if (lastPageNumber > 0u && numPlayers % ENTRIES_PER_PAGE == 0u) {
            lastPageNumber--
        }
        // If user selects out of bounds page, go back to OG.

        val selectedPage =
            if (args.isNotEmpty()) {
                val potentialPage = args[0].toUIntOrNull() ?: 0u
                if (potentialPage > lastPageNumber) {
                    lastPageNumber
                } else potentialPage
            } else 0u

        val formattedOwnersByPoints =
            contentsOfPage(basePlayerList, ENTRIES_PER_PAGE, selectedPage)
            .fold(prefixedMessage(Component.text("Top Players:", NamedTextColor.RED)))
            { acc, (name, points) ->
                acc.append(
                    Component.text("\n - ", NamedTextColor.RED)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" ($points points)", NamedTextColor.GRAY))
                )
            }
            .append(Component.text("\n\nPage $selectedPage of $lastPageNumber", NamedTextColor.GRAY))

        s.sendMessage(formattedOwnersByPoints)
        return true
    }

    /*
     * Internal instance-specific helpers
     */

    /**
     * Retrieves a `Relic` instance based on the provided name arguments or the item held by the sender.
     *
     * This method evaluates the input arguments and determines the appropriate relic using the following logic:
     * - If no arguments are provided:
     *   1. Validates that the sender is a player.
     *   2. Ensures the player is holding exactly one item.
     *   3. Confirms the held item has relic metadata
     *   4. Verifies that the relic's name corresponds to a tracked relic.
     *   If all validations pass, the corresponding relic is returned. Otherwise, it returns `null`.
     *
     * - If one argument is supplied:
     *   Converts the argument from underscore-separated to space-separated format, then finds and
     *   retrieves the relic with the corresponding name, if it exists.
     *
     * - If more than one argument is supplied:
     *   Returns `null`, indicating ambiguity in input handling.
     *
     * @return The corresponding `Relic` instance if a valid relic is found through the given input or held item; otherwise, returns `null`.
     */
    private fun getRelicFromImplicitArgument() =
        when (args.size) {
            0 ->
                if (!v.assertValidPlayer()
                    || !v.assertSingleItemHeld()
                    // Deep check used to ensure that this item has the metadata needed to guarantee relic status.
                    || !v.assertHeldItemIsRelic()) {
                    null
                } else {
                    val relic = itemIntegrator.relicFromItem((s as Player).inventory.itemInMainHand)
                    // Final shallow check ensures that a relic with this name is still tracked.
                    // Using a manual /relic deregister, it's possible to have a Relic item refer to a no-longer existant relic
                    if (v.assertNameRefersToRelic(relic.name)) {
                        relic
                    } else null
                }
            1 -> {
                val formattedName = underscoresToSpaces(args[0])
                // Ensure that error message is sent if no relic with that name.
                v.assertNameRefersToRelic(formattedName)
                relicSet.relicNamed(formattedName)
            }

            else -> null
        }

    /*
     * Message formatting helpers.
     */

    /**
     * @param relic Relic to format
     * @return A component mapping the relic's formatting to its owner.
     */
    private fun formatRelicEntry(relic: Relic) =
        Component.text("\n - ", NamedTextColor.RED)
        .append(relic.asDisplayComponent())
            .append(formatOwner(relic) ?: Component.text(" (unclaimed)", NamedTextColor.GRAY))

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
         * Given a name, convert to player UUID.
         */
    private fun getPlayerFromName(name: String): UUID? =
            Bukkit.getOfflinePlayer(name).let {
                if (!it.hasPlayedBefore()) {
                    null
                } else it.uniqueId
            }

} // end relic subcommand executor
} // end relics command