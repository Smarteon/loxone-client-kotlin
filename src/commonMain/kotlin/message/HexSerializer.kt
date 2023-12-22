package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexSerializer : KSerializer<ByteArray> {

    override val descriptor: SerialDescriptor
        = PrimitiveSerialDescriptor("hexString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteArray {
        return Codec.hexToBytes(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        TODO("Not yet implemented")
    }
}
