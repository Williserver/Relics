package net.williserver.relics.commands

import net.williserver.relics.model.RelicSet
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * The RelicsCommand class represents a command related to relics.
 * It provides functionality to manage and execute operations associated with relic-related actions.
 *
 * @param relicSet Set of relics for this session.
 * @author Willmo3
 */
class RelicsCommand(private val relicSet: RelicSet): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        TODO("Not yet implemented")
    }

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
    // TODO: register relic
    // -- given an item in your hand, add its relic name as an NBT tag.
    // -- add its name to the relic list.

    // TODO: claim relic
    // -- given an item in your hand, check if it's a relic
    // -- if so, mark you as the new owner of the relic.

    // TODO: list relics
    // -- report a list of all relics on the server to player.

    // TODO: top players
    // -- report a list of players, sorted by the value of the relics they own.

    // TODO: relic by player.
    // -- given a player, report what relics they own.

} // end relic subcommand executor
} // end relics command