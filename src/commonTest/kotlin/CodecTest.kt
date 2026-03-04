package cz.smarteon.loxkt

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import cz.smarteon.loxkt.app.StatisticEntry
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalStdlibApi::class)
class CodecTest : StringSpec({

    "should decode hex" {
        Codec.hexToBytes("3235") shouldBe byteArrayOf(50, 53)
    }

    "should encode hex" {
        Codec.bytesToHex(byteArrayOf(50, 53)) shouldBe "3235"
    }

    "should read header" {
        val header = Codec.readHeader(Codec.hexToBytes("03000000ab000000"))
        header shouldBe MessageHeader(MessageKind.TEXT, false, 171)
    }

    "should write header" {
        val header = MessageHeader(MessageKind.TEXT, false, 171)
        Codec.writeHeader(header) shouldBe Codec.hexToBytes("03000000ab000000")
    }

    "should read KEEP_ALIVE header" {
        val header = Codec.readHeader(Codec.hexToBytes("0306000000000000"))
        header shouldBe MessageHeader.KEEP_ALIVE
    }

    "should read statistic entries with single output" {
        // 1 entry: timestamp=1661983200 (0x630F5DE0), value=3.2
        val buffer = PlatformBuffer.allocate(12, byteOrder = ByteOrder.LITTLE_ENDIAN)
        buffer.writeUInt(1661983200u)
        buffer.writeDouble(3.2)
        buffer.resetForRead()
        val bytes = buffer.readByteArray(12)

        val entries = Codec.readStatisticEntries(bytes, 1)
        entries shouldHaveSize 1
        entries[0] shouldBe StatisticEntry(1661983200u, listOf(3.2))
    }

    "should read statistic entries with multiple outputs" {
        // 1 entry: timestamp=100, values=[1.0, 2.0]
        val buffer = PlatformBuffer.allocate(20, byteOrder = ByteOrder.LITTLE_ENDIAN)
        buffer.writeUInt(100u)
        buffer.writeDouble(1.0)
        buffer.writeDouble(2.0)
        buffer.resetForRead()
        val bytes = buffer.readByteArray(20)

        val entries = Codec.readStatisticEntries(bytes, 2)
        entries shouldHaveSize 1
        entries[0] shouldBe StatisticEntry(100u, listOf(1.0, 2.0))
    }

    "should return empty list for empty statistic data" {
        Codec.readStatisticEntries(byteArrayOf(), 1).shouldBeEmpty()
    }

    "should return empty list for zero output count" {
        Codec.readStatisticEntries(byteArrayOf(0, 0, 0, 0), 0).shouldBeEmpty()
    }

    "should convert loxone local datetime to timestamp using UTC" {
        // 2009-01-01 00:00:00 UTC is Loxone epoch, so 2009-01-01 01:00:00 UTC = 3600 seconds
        val utc = TimeZone.UTC
        Codec.loxoneLocalDateTimeToTimestamp("2009-01-01 01:00:00", utc) shouldBe 3600u
    }

    "should produce different absolute timestamps for same wall-clock string in different timezones" {
        val utc = TimeZone.UTC
        val vienna = TimeZone.of("Europe/Vienna")
        val tsUtc = Codec.loxoneLocalDateTimeToTimestamp("2023-08-01 12:00:00", utc)
        val tsVienna = Codec.loxoneLocalDateTimeToTimestamp("2023-08-01 12:00:00", vienna)
        // UTC has no DST offset change; Vienna's epoch (winter, UTC+1) vs sample (summer, UTC+2)
        // means the Vienna delta is 3600 less than the UTC delta.
        (tsUtc - tsVienna) shouldBe 3600u
    }

    "should read multiple statistic entries" {
        // 2 entries with 1 output each: 4 (ts) + 8 (value) = 12 bytes each = 24 bytes total
        val buffer = PlatformBuffer.allocate(24, byteOrder = ByteOrder.LITTLE_ENDIAN)
        buffer.writeUInt(1000u)
        buffer.writeDouble(10.5)
        buffer.writeUInt(2000u)
        buffer.writeDouble(20.5)
        buffer.resetForRead()
        val bytes = buffer.readByteArray(24)

        val entries = Codec.readStatisticEntries(bytes, 1)
        entries shouldHaveSize 2
        entries[0] shouldBe StatisticEntry(1000u, listOf(10.5))
        entries[1] shouldBe StatisticEntry(2000u, listOf(20.5))
    }
})
