package cz.smarteon.loxone

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.wrap
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageHeader.Companion.FIRST_BYTE
import cz.smarteon.loxone.message.MessageHeader.Companion.PAYLOAD_LENGTH
import cz.smarteon.loxone.message.MessageKind
import kotlinx.serialization.json.Json

object Codec {

    private const val SEPARATOR = ':'

    val loxJson = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun hexToBytes(hex: String): ByteArray = hex.hexToByteArray(HexFormat.UpperCase)

    @OptIn(ExperimentalStdlibApi::class)
    fun bytesToHex(bytes: ByteArray, format: HexFormat = HexFormat.Default): String
        = bytes.toHexString(format)

    internal fun concat(first: String, second: String): String {
        return first + SEPARATOR + second
    }

    internal fun concatToBytes(first: String, second: String): ByteArray {
        return concat(first, second).encodeToByteArray()
    }

    internal fun readHeader(bytes: ByteArray): MessageHeader {
        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        buffer.position(0)
        val limit: Int = buffer.limit()
        val first: Byte = buffer.readByte()
        return if (limit != PAYLOAD_LENGTH || first != FIRST_BYTE) {
            throw LoxoneException(
                "Payload is not a valid loxone message header, size="
                    + limit + ", firstByte=" + first
            )
        } else {
            MessageHeader(
                MessageKind.entries[buffer.readByte().toInt()],
                (buffer.readByte().toInt() and 1) == 1,
                buffer.readUnsignedInt().toLong()
            )
        }
    }

}
