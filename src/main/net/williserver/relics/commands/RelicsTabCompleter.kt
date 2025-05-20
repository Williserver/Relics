package net.williserver.relics.commands

import net.williserver.relics.model.RelicSet
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import net.williserver.relics.commands.RelicsCommand.Companion.spacesToUnderscores

/**
 * State for relics tab completion.
 * @param relicSet Set of relics for this session. Used to auto-complete relic names.
 *
 * @author Willmo3
 */
class RelicsTabCompleter(val relicSet: RelicSet): TabCompleter {
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
            1 -> {
                completions.addAll(setOf("deregister", "help", "list", "register"))
                completions.removeAll{ !it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "register" ->
                        completions.addAll(setOf("common", "rare", "epic", "legendary", "unique"))
                    "deregister" ->
                        relicSet.relics().forEach { relic -> completions.add(spacesToUnderscores(relic.name)) }
                }
                completions.removeAll{ !it.startsWith(args[1], ignoreCase = true) }
            }
        }

        return completions
    }
}