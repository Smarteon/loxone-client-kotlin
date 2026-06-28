package cz.smarteon.loxkt

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import com.ditchoom.buffer.wrap
import cz.smarteon.loxkt.app.StatisticEntry
import cz.smarteon.loxkt.event.DaytimerEntry
import cz.smarteon.loxkt.event.DaytimerEvent
import cz.smarteon.loxkt.event.TextEvent
import cz.smarteon.loxkt.event.ValueEvent
import cz.smarteon.loxkt.event.WeatherEntry
import cz.smarteon.loxkt.event.WeatherEvent
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageHeader.Companion.FIRST_BYTE
import cz.smarteon.loxkt.message.MessageHeader.Companion.MSG_SIZE_POSITION
import cz.smarteon.loxkt.message.MessageHeader.Companion.PAYLOAD_LENGTH
import cz.smarteon.loxkt.message.MessageKind
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json

@Suppress("TooManyFunctions")
object Codec {

    private val logger = KotlinLogging.logger {}
    private const val SEPARATOR = ':'
    private const val UUID_DATA1_LENGTH = 8
    private const val UUID_DATA2_LENGTH = 4
    private const val UUID_DATA4_SIZE = 8
    private const val TEXT_PADDING_ALIGNMENT = 4

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

    /**
     * Reads a UUID from the buffer at the current position.
     * UUID is 128 bits (16 bytes) in little-endian format:
     * - Data1: 32-bit unsigned int
     * - Data2: 16-bit unsigned int
     * - Data3: 16-bit unsigned int
     * - Data4: 8 bytes
     *
     * @param buffer Buffer positioned at the start of the UUID
     * @return UUID string in format "xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxx"
     */
    internal fun readUuid(buffer: PlatformBuffer): String {
        val data1 = buffer.readUnsignedInt()
        val data2 = buffer.readUnsignedShort()
        val data3 = buffer.readUnsignedShort()
        val data4 = ByteArray(UUID_DATA4_SIZE)
        for (i in 0 until UUID_DATA4_SIZE) {
            data4[i] = buffer.readByte()
        }

        return buildString {
            append(data1.toString(radix = 16).padStart(UUID_DATA1_LENGTH, '0'))
            append('-')
            append(data2.toString(radix = 16).padStart(UUID_DATA2_LENGTH, '0'))
            append('-')
            append(data3.toString(radix = 16).padStart(UUID_DATA2_LENGTH, '0'))
            append('-')
            for (i in 0 until UUID_DATA4_SIZE) {
                append(data4[i].toHexString())
            }
        }
    }

    /**
     * Reads value events from binary data.
     * Value-States consist of UUID (16 bytes) + double value (8 bytes) = 24 bytes each.
     *
     * @param bytes Binary data containing value event table
     * @return List of parsed value events
     */
    internal fun readValueEvents(bytes: ByteArray): List<ValueEvent> {
        if (bytes.isEmpty()) return emptyList()

        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        val events = mutableListOf<ValueEvent>()

        while (buffer.remaining() >= ValueEvent.SIZE_BYTES) {
            val uuid = readUuid(buffer)
            val value = buffer.readDouble()
            events.add(ValueEvent(uuid, value))
        }

        return events
    }

    /**
     * Reads text events from binary data.
     * Text-States have variable size: UUID (16) + Icon UUID (16) + text length (4) + text (padded to 4 bytes).
     *
     * @param bytes Binary data containing text event table
     * @return List of parsed text events
     */
    internal fun readTextEvents(bytes: ByteArray): List<TextEvent> {
        if (bytes.isEmpty()) return emptyList()

        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        val events = mutableListOf<TextEvent>()

        while (buffer.remaining() >= TextEvent.HEADER_SIZE_BYTES) {
            val uuid = readUuid(buffer)
            val iconUuid = readUuid(buffer)
            val textLength = buffer.readUnsignedInt().toInt()

            // Calculate padding (text is padded to multiple of 4)
            val padding = (TEXT_PADDING_ALIGNMENT - (textLength % TEXT_PADDING_ALIGNMENT)) % TEXT_PADDING_ALIGNMENT
            val totalTextBytes = textLength + padding

            if (buffer.remaining() < totalTextBytes) break

            val textBytes = ByteArray(textLength)
            for (i in 0 until textLength) {
                textBytes[i] = buffer.readByte()
            }
            val text = textBytes.decodeToString().trimEnd('\u0000')

            // Skip padding bytes (text is padded to multiple of 4)
            repeat(padding) {
                buffer.readByte()
            }

            events.add(TextEvent(uuid, iconUuid, text))
        }

        return events
    }

    /**
     * Reads daytimer events from binary data.
     * Daytimer-States: UUID (16) + default value (8) + entry count (4) + entries (24 each).
     *
     * @param bytes Binary data containing daytimer event table
     * @return List of parsed daytimer events
     */
    internal fun readDaytimerEvents(bytes: ByteArray): List<DaytimerEvent> {
        if (bytes.isEmpty()) return emptyList()

        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        val events = mutableListOf<DaytimerEvent>()

        while (buffer.remaining() >= DaytimerEvent.HEADER_SIZE_BYTES) {
            val uuid = readUuid(buffer)
            val defaultValue = buffer.readDouble()
            val entryCount = buffer.readInt()

            if (entryCount < 0 || buffer.remaining().toLong() < entryCount.toLong() * DaytimerEntry.SIZE_BYTES) break

            val entries = mutableListOf<DaytimerEntry>()
            repeat(entryCount) {
                val mode = buffer.readInt()
                val from = buffer.readInt()
                val to = buffer.readInt()
                val needActivate = buffer.readInt() != 0
                val value = buffer.readDouble()
                entries.add(DaytimerEntry(mode, from, to, needActivate, value))
            }

            events.add(DaytimerEvent(uuid, defaultValue, entries))
        }

        return events
    }

    /**
     * Reads statistic entries from binary data.
     * Each entry consists of: UInt32 timestamp (4 bytes) + N × Float64 values (8 bytes each).
     *
     * For V1: timestamp is seconds since 1.1.2009 in local Miniserver-time.
     * For V2: timestamp is unix UTC timestamp.
     *
     * @param bytes Binary data containing statistic entries
     * @param outputCount Number of values per entry (from statistic outputs/dataPoints count).
     *   When null, the count is inferred from the binary data size.
     * @return List of statistic entries
     */
    fun readStatisticEntries(bytes: ByteArray, outputCount: Int? = null): List<StatisticEntry> {
        val count = resolveOutputCount(bytes, outputCount) ?: return emptyList()
        val entrySize = UINT32_SIZE + count * FLOAT64_SIZE
        if (bytes.size % entrySize != 0) {
            logger.warn {
                "Statistic data size ${bytes.size} is not a multiple of entry size $entrySize " +
                    "(${bytes.size % entrySize} trailing bytes)"
            }
        }
        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        return buildList {
            while (buffer.remaining() >= entrySize) {
                val timestamp = buffer.readUnsignedInt()
                val values = List(count) { buffer.readDouble() }
                add(StatisticEntry(timestamp, values))
            }
        }
    }

    /** Resolves and validates the output count, returning null if the data should be skipped. */
    private fun resolveOutputCount(bytes: ByteArray, outputCount: Int?): Int? {
        if (bytes.isEmpty()) return null
        val count = outputCount ?: inferOutputCount(bytes)
        return count?.takeIf { it > 0 }
    }

    /** Returns the output count that evenly divides [bytes], or null if none found. */
    private fun inferOutputCount(bytes: ByteArray): Int? {
        for (n in 1..MAX_OUTPUT_COUNT) {
            if (bytes.size % (UINT32_SIZE + n * FLOAT64_SIZE) == 0) return n
        }
        return null
    }

    /**
     * Reads weather events from binary data.
     * Weather-States: UUID (16) + last update (4) + entry count (4) + entries (68 each).
     *
     * @param bytes Binary data containing weather event table
     * @return List of parsed weather events
     */
    internal fun readWeatherEvents(bytes: ByteArray): List<WeatherEvent> {
        if (bytes.isEmpty()) return emptyList()

        val buffer = PlatformBuffer.wrap(bytes, ByteOrder.LITTLE_ENDIAN)
        val events = mutableListOf<WeatherEvent>()

        while (buffer.remaining() >= WeatherEvent.HEADER_SIZE_BYTES) {
            val uuid = readUuid(buffer)
            val lastUpdate = buffer.readUnsignedInt().toInt()
            val entryCount = buffer.readInt()

            if (entryCount < 0 || buffer.remaining().toLong() < entryCount.toLong() * WeatherEntry.SIZE_BYTES) break

            val entries = mutableListOf<WeatherEntry>()
            repeat(entryCount) {
                val timestamp = buffer.readInt()
                val weatherType = buffer.readInt()
                val windDirection = buffer.readInt()
                val solarRadiation = buffer.readInt()
                val relativeHumidity = buffer.readInt()
                val temperature = buffer.readDouble()
                val perceivedTemperature = buffer.readDouble()
                val dewPoint = buffer.readDouble()
                val precipitation = buffer.readDouble()
                val windSpeed = buffer.readDouble()
                val barometricPressure = buffer.readDouble()
                entries.add(
                    WeatherEntry(
                        timestamp,
                        weatherType,
                        windDirection,
                        solarRadiation,
                        relativeHumidity,
                        temperature,
                        perceivedTemperature,
                        dewPoint,
                        precipitation,
                        windSpeed,
                        barometricPressure
                    )
                )
            }

            events.add(WeatherEvent(uuid, lastUpdate, entries))
        }

        return events
    }

    private const val UINT32_SIZE = 4
    private const val FLOAT64_SIZE = 8
    private const val MAX_OUTPUT_COUNT = 8
    private const val LOXONE_EPOCH_YEAR = 2009
    private const val LOXONE_EPOCH_MONTH = 1
    private const val LOXONE_EPOCH_DAY = 1

    // Loxone epoch as a local datetime — intentionally without timezone, so it is
    // interpreted in the same timezone as the parsed miniserver timestamps.
    private val LOXONE_EPOCH_LOCAL = LocalDateTime(LOXONE_EPOCH_YEAR, LOXONE_EPOCH_MONTH, LOXONE_EPOCH_DAY, 0, 0, 0)

    /**
     * Converts a Loxone local datetime string "YYYY-MM-DD HH:MM:SS" to a UInt timestamp.
     * Result is seconds since the Loxone epoch (2009-01-01 00:00:00) in local Miniserver time.
     *
     * @param dateTime Miniserver-local datetime string in the format "YYYY-MM-DD HH:MM:SS"
     * @param timeZone The timezone the Miniserver operates in. Defaults to [TimeZone.currentSystemDefault].
     *   Pass the Miniserver's actual timezone (e.g. obtained from its location/config) to avoid
     *   incorrect offsets when the client runs in a different timezone.
     */
    internal fun loxoneLocalDateTimeToTimestamp(
        dateTime: String,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): UInt {
        val ldt = LocalDateTime.parse(dateTime.replace(' ', 'T'))
        return (ldt.toInstant(timeZone).epochSeconds - LOXONE_EPOCH_LOCAL.toInstant(timeZone).epochSeconds).toUInt()
    }

    /**
     * Converts an [Instant] to a V1 Loxone-epoch timestamp (UInt seconds since 2009-01-01 00:00:00
     * local Miniserver time), using the same convention as [loxoneLocalDateTimeToTimestamp].
     *
     * Used to convert UTC `from`/`until` bounds into the same unit as V1 [cz.smarteon.loxkt.app.StatisticEntry]
     * timestamps so that range-filtering is consistent regardless of timezone.
     *
     * @param instant The point in time to convert
     * @param timeZone The Miniserver's timezone
     */
    internal fun instantToLoxoneTimestamp(instant: Instant, timeZone: TimeZone): UInt =
        (instant.epochSeconds - LOXONE_EPOCH_LOCAL.toInstant(timeZone).epochSeconds).toUInt()
}
