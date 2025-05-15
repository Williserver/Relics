package net.williserver.relics.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * State for relics tab completion.
 *
 * @author Willmo3
 */
class RelicsTabCompleter: TabCompleter {
    /**
     * Tab completion suggestions for relics command.
     *
     * @param sender The source of the command (e.g., a player or console sender).
     * @param command The command being tab-completed.
     * @param alias The alias of the command used.
     * @param args The current arguments provided by the user for the command.
     * @return A mutable list of strings containing suggested tab completions for the command.
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val completions = mutableListOf<String>()

        if (!command.name.equals("relics", ignoreCase = true)) {
            return completions
        }

        // Subcommand suggestions
        when (args.size) {
            1 -> completions.add("help")
        }

        return completions
    }
}