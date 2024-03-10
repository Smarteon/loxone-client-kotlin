package cz.smarteon.loxone

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import com.ditchoom.buffer.wrap
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageHeader.Companion.FIRST_BYTE
import cz.smarteon.loxone.message.MessageHeader.Companion.MSG_SIZE_POSITION
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
    fun bytesToHex(bytes: ByteArray, format: HexFormat = HexFormat.Default): String = bytes.toHexString(format)

    internal fun concat(first: String, second: String): String {
        return first + SEPARATOR + second
    }

    internal fun concatToBytes(first: String, second: String): ByteArray {
        return concat(first, second).encodeToByteArray()
    }

    /**
     * Reads the header of the message from the given bytes. According to the loxone specification.
     * Loxone uses little endian byte order.
     *  * 1st byte - always 0x03 ([MessageHeader.FIRST_BYTE]), otherwise [LoxoneException] is thrown
     *  * 2nd byte - message kind - represents the ordinal of [MessageKind]
     *  * 3rd byte - bitset of flags, only the first bit used for size estimated
     *  * 4th byte - is a reserve (not parsed)
     *  * 5th-8th byte - message size as unsigned int
     * @param bytes to read the header from
     */
    internal fun readHeader(bytes: ByteArray): MessageHeader {
        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        val limit: Int = buffer.limit()
        val first: Byte = buffer[0]
        return if (limit != PAYLOAD_LENGTH || first != FIRST_BYTE) {
            throw LoxoneException("Payload is not a valid loxone message header, size=$limit, firstByte=$first")
        } else {
            MessageHeader(
                MessageKind.entries[buffer[1].toInt()],
                (buffer[2].toInt() and 1) == 1,
                buffer.getUnsignedInt(MSG_SIZE_POSITION).toLong()
            )
        }
    }

    internal fun writeHeader(header: MessageHeader): ByteArray {
        val buffer = PlatformBuffer.allocate(PAYLOAD_LENGTH, byteOrder = ByteOrder.LITTLE_ENDIAN)
        buffer[0] = FIRST_BYTE
        buffer[1] = header.kind.ordinal.toByte()
        buffer[2] = (if (header.sizeEstimated) 1 else 0).toByte()
        buffer[MSG_SIZE_POSITION] = header.messageSize.toUInt()
        return buffer.readByteArray(PAYLOAD_LENGTH)
    }
}
