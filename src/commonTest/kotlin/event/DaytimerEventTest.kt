package cz.smarteon.loxkt.event

import cz.smarteon.loxkt.Codec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [Codec.readDaytimerEvents].
 *
 * Daytimer events have variable size:
 * - UUID: 16 bytes (little endian)
 * - Default value: 8 bytes (double, little endian)
 * - Entry count: 4 bytes (int32, little endian)
 * - Entries: 24 bytes each (mode: 4, from: 4, to: 4, needActivate: 4, value: 8)
 *
 * Minimum size: 28 bytes (header only, no entries)
 */
class DaytimerEventTest : ShouldSpec({

    context("readDaytimerEvents") {
        should("return empty list for empty input") {
            Codec.readDaytimerEvents(ByteArray(0)).shouldBeEmpty()
        }

        should("parse daytimer event with no entries") {
            // UUID: 11111111-2222-3333-4444444444444444
            // Default value: 21.0 (0x4035000000000000 as double)
            // Entry count: 0
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val defaultValue = "0000000000003540" // 21.0 little endian
            val entryCount = "00000000"
            val bytes = (uuid + defaultValue + entryCount).hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "11111111-2222-3333-4444444444444444"
            events[0].defaultValue shouldBeExactly 21.0
            events[0].entries.shouldBeEmpty()
        }

        should("parse daytimer event with single entry") {
            // UUID: aaaaaaaa-bbbb-cccc-dddddddddddddddd
            // Default value: 18.0
            // Entry count: 1
            // Entry: mode=1, from=360 (6:00), to=480 (8:00), needActivate=0, value=22.0
            val uuid = "aaaaaaaa" + "bbbb" + "cccc" + "dddddddddddddddd"
            val defaultValue = "0000000000003240" // 18.0 little endian
            val entryCount = "01000000" // 1

            // Entry 1: mode=1, from=360, to=480, needActivate=0, value=22.0
            val entry1Mode = "01000000"
            val entry1From = "68010000" // 360 little endian
            val entry1To = "e0010000" // 480 little endian
            val entry1NeedActivate = "00000000"
            val entry1Value = "0000000000003640" // 22.0 little endian

            val bytes = (uuid + defaultValue + entryCount +
                         entry1Mode + entry1From + entry1To + entry1NeedActivate + entry1Value).hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "aaaaaaaa-bbbb-cccc-dddddddddddddddd"
            events[0].defaultValue shouldBeExactly 18.0
            events[0].entries shouldHaveSize 1

            val entry = events[0].entries[0]
            entry.mode shouldBe 1
            entry.from shouldBe 360  // 6:00
            entry.to shouldBe 480    // 8:00
            entry.needActivate shouldBe false
            entry.value shouldBeExactly 22.0
        }

        should("parse daytimer event with multiple entries") {
            // UUID: 12345678-1234-5678-0000000000000000
            // Default value: 15.0
            // 3 entries covering a full day schedule
            val uuid = "78563412" + "3412" + "7856" + "0000000000000000"
            val defaultValue = "0000000000002e40" // 15.0 little endian
            val entryCount = "03000000" // 3

            // Entry 1: mode=0, from=0 (00:00), to=360 (06:00), needActivate=0, value=18.0
            val entry1 = "00000000" + "00000000" + "68010000" + "00000000" + "0000000000003240"

            // Entry 2: mode=0, from=360 (06:00), to=1080 (18:00), needActivate=1, value=22.0
            val entry2 = "00000000" + "68010000" + "38040000" + "01000000" + "0000000000003640"

            // Entry 3: mode=0, from=1080 (18:00), to=1440 (24:00), needActivate=0, value=18.0
            val entry3 = "00000000" + "38040000" + "a0050000" + "00000000" + "0000000000003240"

            val bytes = (uuid + defaultValue + entryCount + entry1 + entry2 + entry3).hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events shouldHaveSize 1
            events[0].entries shouldHaveSize 3

            // Verify entry times are parsed correctly
            events[0].entries[0].fromTimeString() shouldBe "00:00"
            events[0].entries[0].toTimeString() shouldBe "06:00"
            events[0].entries[1].fromTimeString() shouldBe "06:00"
            events[0].entries[1].toTimeString() shouldBe "18:00"
            events[0].entries[1].needActivate shouldBe true
            events[0].entries[2].fromTimeString() shouldBe "18:00"
            events[0].entries[2].toTimeString() shouldBe "24:00"
        }

        should("handle truncated header gracefully") {
            // Only 20 bytes - less than minimum header size of 28
            val partialHeader = "11111111" + "2222" + "3333" + "4444444444444444" + "00000000"
            val bytes = partialHeader.hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events.shouldBeEmpty()
        }

        should("handle truncated entries gracefully") {
            // Header says 2 entries but only space for 1
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val defaultValue = "0000000000003540"
            val entryCount = "02000000" // Claims 2 entries

            // Only 1 complete entry (24 bytes)
            val entry1 = "00000000" + "00000000" + "68010000" + "00000000" + "0000000000003240"

            val bytes = (uuid + defaultValue + entryCount + entry1).hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events.shouldBeEmpty()
        }

        should("handle negative entry count gracefully") {
            // Header with negative entry count (-1 = 0xFFFFFFFF)
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val defaultValue = "0000000000003540"
            val entryCount = "ffffffff" // -1 in little endian signed int

            val bytes = (uuid + defaultValue + entryCount).hexToByteArray()

            val events = Codec.readDaytimerEvents(bytes)

            events.shouldBeEmpty()
        }
    }
})
