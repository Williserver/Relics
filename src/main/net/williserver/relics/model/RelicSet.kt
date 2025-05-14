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
import java.io.FileWriter
import java.io.File
import java.io.FileReader
import java.util.UUID

/**
 * Set of tracked relics and their owners, if they have one.
 *
 * @author Willmo3
 */
@Serializable
class RelicSet(private val relicsToOwner: MutableMap<Relic, SUUID> = mutableMapOf()) {
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
            relicsToOwner.put(relic, null)
        }

    /*
     * Accessors
     */

    /**
     * @return an immutable set view of all relics tracked by this plugin.
     */
    fun relics() = relicsToOwner.keys.toSet()

    /*
     * Comparison helpers.
     */

    override fun equals(other: Any?) = other is RelicSet && other.relics() == relics()

    override fun hashCode(): Int {
        return relicsToOwner.hashCode()
    }

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