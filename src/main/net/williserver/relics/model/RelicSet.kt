package net.williserver.relics.model

import kotlinx.serialization.json.Json
import net.williserver.relics.LogHandler
import net.williserver.relics.RelicsPlugin.Companion.PLUGIN_MESSAGE_PREFIX
import kotlinx.serialization.Serializable
import java.io.FileWriter
import java.io.File
import java.io.FileReader

/**
 * Set of tracked relics and their owners, if they have one.
 *
 * @author Willmo3
 */
@Serializable
class RelicSet(private val allRelics: MutableSet<Relic> = mutableSetOf()) {
    /*
     * Mutators
     */

    /**
     * @param relic new relic to register in this set.
     * @throws IllegalArgumentException if this relic is identical to another which has already been registered.
     */
    fun register(relic: Relic) =
        if (relic in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has already been registered!")
        } else {
            allRelics += relic
        }

    /*
     * Accessors
     */

    /**
     * @return an immutable set view of all relics tracked by this plugin.
     */
    fun relics() = allRelics.toSet()

    /*
     * Comparison helpers.
     */

    override fun equals(other: Any?) = other is RelicSet && other.relics() == relics()

    override fun hashCode(): Int {
        return allRelics.hashCode()
    }

    /*
     * File I/O helpers
     */

    companion object {
        /**
         * Writes the provided relics set to a file in JSON format and logs the operation.
         *
         * @param logger the logger used to report information about the write operation.
         * @param path the file path where the relic set will be written.
         * @param relicSet the relic set to be serialized and written to the file.
         */
        fun writeToFile(logger: LogHandler, path: String, relicSet: RelicSet) {
            val writer = FileWriter(path)
            writer.write(Json.encodeToString(relicSet))
            writer.close()
            logger.info("Relic set written to file.")
        }

        /**
         * Reads a relic set from a JSON file at the specified path. If the file does not exist,
         * an empty relic set is created and returned. Logs relevant information during the process.
         *
         * @param logger the logger used to log messages about the operation.
         * @param path the file path from which the relic set is to be read.
         * @return the relic set read from the file, or a new empty set if no file exists.
         */
        fun readFromFile(logger: LogHandler, path: String) : RelicSet {
            if (!File(path).exists()) {
                logger.info("Found no relic set at $path, returning new empty set.")
                return RelicSet()
            }

            val jsonString = FileReader(path).readText()
            val relicSet = Json.decodeFromString<RelicSet>(jsonString)
            logger.info("Relic set loaded from file.")
            return relicSet
        }
    }
}