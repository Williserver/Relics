package net.williserver.relics.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import java.util.UUID
import kotlin.reflect.typeOf

/**
 * Relics are associated with a rarity.
 */
@Serializable
enum class RelicRarity() {
    Common,
    Rare,
    Epic,
    Legendary,
    Unique
}

/**
 * A named relic. Contains a UUID, a rarity, and a name.
 *
 * @author Willmo3
 */
@Serializable(with= RelicSerializer::class)
data class Relic(val id: UUID, val rarity: RelicRarity, val name: String)

/**
 * Custom serializer for a relic that reads UUIDs in, out.
 *
 * @author Willmo3
 */
object RelicSerializer : KSerializer<Relic> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Relic") {
        element<String>("id")
        element<RelicRarity>("rarity")
        element<String>("name")
    }

    /*
     * Credit to https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#handwritten-composite-serializer
     */

    override fun serialize(encoder: Encoder, value: Relic) =
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, index = 0, value.id.toString())
            encodeSerializableElement(descriptor, index = 1, serializer<RelicRarity>(), value.rarity)
            encodeStringElement(descriptor, index = 2, value.name)
        }

    override fun deserialize(decoder: Decoder): Relic =
        decoder.decodeStructure(descriptor) {
            var id: UUID? = null
            var rarity: RelicRarity? = null
            var name: String? = null
            // Decode elements in an arbitrary order
            while (true) {
                when (val field = decodeElementIndex(descriptor)) {
                    0 -> id = UUID.fromString(decodeStringElement(descriptor, 0))
                    1 -> rarity = decodeSerializableElement(descriptor, index = 1, serializer<RelicRarity>())
                    2 -> name = decodeStringElement(descriptor, 2)
                    DECODE_DONE -> break
                    else -> throw IllegalStateException("Unexpected field for relic: $field")
                }
            }
            if (id == null || rarity == null || name == null) {
                throw IllegalStateException("Incomplete relic Json object.")
            }
            Relic(id, rarity, name)
        }
}