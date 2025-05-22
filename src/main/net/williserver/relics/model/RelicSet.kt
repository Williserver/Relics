package net.williserver.relics.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import net.williserver.relics.LogHandler
import net.williserver.relics.RelicsPlugin.Companion.PLUGIN_MESSAGE_PREFIX
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.williserver.relics.session.RelicLifecycleListener
import java.io.FileWriter
import java.io.File
import java.io.FileReader
import java.util.SortedSet
import java.util.UUID
import kotlin.comparisons.compareByDescending

/**
 * Represents a collection of relics and their ownership information. Provides functionality to register,
 * claim, and retrieve relics, as well as to handle file operations for persistence.
 *
 * @constructor Initializes an empty relic set or populates it from the provided map of relics to owners.
 * @param relicsToOwner A map associating relics to their owners, with null indicating unclaimed relics.
 * @author Willmo3
 */
@Serializable
class RelicSet(private val relicsToOwner: MutableMap<Relic, SUUID> = mutableMapOf()) {
    /*
     * Mutators
     */

    /**
     * Registers a relic in the set, if it has not already been registered.
     *
     * @param relic The relic to be added to the set. Must not already exist in the set.
     * @throws IllegalArgumentException if the relic has already been registered.
     */
    fun register(relic: Relic) =
        if (relic in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has already been registered!")
        } else {
            relicsToOwner.put(relic, null)
        }

    /**
     * Assigns an owner to a relic, marking it as claimed if it has not already been claimed or registered.
     *
     * @param relic The relic to be claimed by the owner.
     * @param owner The UUID of the owner claiming the relic.
     * @throws IllegalArgumentException if the relic is not registered in the set.
     */
    fun claim(relic: Relic, owner: UUID) =
        if (relic !in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has not been registered!")
        } else {
            relicsToOwner[relic] = owner
        }

    /**
     * Removes a relic from the set of registered relics, effectively destroying it.
     * If the relic is not registered, an exception is thrown.
     *
     * @param relic The relic to be removed from the set. Must be a registered relic.
     * @throws IllegalArgumentException if the relic has not been registered in the set.
     */
    fun destroy(relic: Relic) =
        if (relic !in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has not been registered!")
        } else relicsToOwner -= relic

    /*
     * Accessors
     */

    /**
     * @param relic The relic whose owner is to be retrieved. Must be a registered relic.
     * @return The UUID of the owner associated with the relic, or null if the relic has no owner.
     * @throws IllegalArgumentException if the relic is not registered in the set.
     */
    fun ownerOf(relic: Relic): SUUID? =
        if (relic !in relics()) {
            throw IllegalArgumentException("$PLUGIN_MESSAGE_PREFIX: this relic has not been registered!")
        } else relicsToOwner[relic]

    /**
     * @param name The name of the relic to search for. Must match the name of the relic exactly.
     * @return The matching relic if found, or null if no relic with the specified name exists.
     */
    fun relicNamed(name: String) = relics().find { it.name == name }

    /**
     * @param relic The relic to check for membership in the set.
     * @return whether the relic is in this set.
     */
    operator fun contains(relic: Relic) = relic in relicsToOwner.keys

    /**
     * @return an immutable set view of all relics tracked by this plugin.
     */
    fun relics() = relicsToOwner.keys.toSet()

    /**
     * @return an immutable set view of all relics owned by some player in this plugin.
     */
    fun ownedRelics() = relics().filter { ownerOf(it) != null }.toSet()

    /**
     * @return A map of players to the point total of all the relics they own.
     */
    fun playersToRelicPoints(): Map<UUID, UInt> =
        ownedRelics()
        .fold(mutableMapOf())
            { sums, currentRelic ->
                sums[ownerOf(currentRelic)!!] = sums.getOrDefault(ownerOf(currentRelic), 0u) + currentRelic.rarity.points()
                sums
            }

    /*
     * Listeners
     */

    /**
     * @return A RelicLifecycleListener instance that registers relics upon invocation.
     */
    fun constructRegisterListener(): RelicLifecycleListener = { relic, _, _ -> register(relic) }

    /**
     * @return A RelicLifecycleListener that assigns an owner to a relic when triggered.
     */
    fun constructClaimListener(): RelicLifecycleListener = { relic, newOwner, _ -> claim(relic, newOwner!!) }

    /**
     * @return A RelicLifecycleListener that handles the destruction of relics.
     */
    fun constructDestroyListener(): RelicLifecycleListener = { relic, _, _ -> destroy(relic) }

    /*
     * Comparison helpers.
     */

    override fun equals(other: Any?) = other is RelicSet && other.relics() == relics()

    override fun hashCode() = relicsToOwner.hashCode()

    /*
     * File I/O helpers
     */

    companion object {
        private val format = Json { allowStructuredMapKeys=true; prettyPrint=true }

        /**
         * Writes the provided relics set to a file in JSON format and logs the operation.
         *
         * @param logger the logger used to report information about the write operation.
         * @param path the file path where the relic set will be written.
         * @param relicSet the relic set to be serialized and written to the file.
         */
        fun writeToFile(logger: LogHandler, path: String, relicSet: RelicSet) {
            val writer = FileWriter(path)
            writer.write(format.encodeToString(relicSet))
            writer.close()
            logger.info("Relic set written to file at path $path.")
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

            val reader = FileReader(path)
            val jsonString = reader.readText()
            reader.close()
            logger.info("Found relic set at $path, loading from file.")

            val relicSet = format.decodeFromString<RelicSet>(jsonString)
            logger.info("Relic set loaded from file.")
            return relicSet
        }
    }
}


/**
 * A typealias for a nullable UUID with a custom serializer for Kotlin serialization.
 * Represents an optional UUID field serialized with `optionalUUIDSerializer`.
 * If the UUID is null, it is serialized as the string "none".
 */
typealias SUUID = @Serializable(with = OptionalUUIDSerializer::class) UUID?

/**
 * Custom serializer for a nullable UUID.
 * Credit: https://github.com/perracodex/Kotlinx-UUID-Serializer
 *
 * @author Willmo3
 */
object OptionalUUIDSerializer : KSerializer<UUID?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID?) =
        if (value == null) {
            encoder.encodeString("none")
        } else {
            encoder.encodeString(value.toString())
        }

    override fun deserialize(decoder: Decoder): UUID? {
        val decodedString = decoder.decodeString()
        return if (decodedString == "none") {
            null
        } else {
            UUID.fromString(decodedString)
        }
    }
}