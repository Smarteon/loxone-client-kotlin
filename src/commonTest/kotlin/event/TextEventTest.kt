package cz.smarteon.loxkt.event

import cz.smarteon.loxkt.Codec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [Codec.readTextEvents].
 *
 * Text events have variable size:
 * - UUID: 16 bytes (little endian)
 * - Icon UUID: 16 bytes (little endian)
 * - Text length: 4 bytes (uint32, little endian)
 * - Text: variable length (padded to multiple of 4 bytes)
 *
 * Minimum size: 36 bytes (header only, no text)
 */
@OptIn(ExperimentalStdlibApi::class)
class TextEventTest : ShouldSpec({

    context("readTextEvents") {
        should("return empty list for empty input") {
            Codec.readTextEvents(ByteArray(0)).shouldBeEmpty()
        }

        should("parse text event with simple text") {
            // UUID: 11111111-2222-3333-4444444444444444
            // Icon UUID: aaaaaaaa-bbbb-cccc-dddddddddddddddd
            // Text: "Hello" (5 bytes, padded to 8)
            val uuid = "11111111" + "2222" + "3333" + "4444444444444444"
            val iconUuid = "aaaaaaaa" + "bbbb" + "cccc" + "dddddddddddddddd"
            val textLength = "05000000" // 5 in little endian
            val textData = "48656c6c6f" + "000000" // "Hello" + 3 bytes padding
            val bytes = (uuid + iconUuid + textLength + textData).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "11111111-2222-3333-4444444444444444"
            events[0].iconUuid shouldBe "aaaaaaaa-bbbb-cccc-dddddddddddddddd"
            events[0].text shouldBe "Hello"
        }

        should("parse text event with text that needs no padding") {
            // Text: "test" (4 bytes, no padding needed)
            val uuid = "12345678" + "1234" + "5678" + "0000000000000000"
            val iconUuid = "00000000" + "0000" + "0000" + "0000000000000000"
            val textLength = "04000000" // 4 in little endian
            val textData = "74657374" // "test" - exactly 4 bytes, no padding
            val bytes = (uuid + iconUuid + textLength + textData).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 1
            events[0].text shouldBe "test"
        }

        should("parse text event with empty text") {
            val uuid = "12345678" + "1234" + "5678" + "0000000000000000"
            val iconUuid = "00000000" + "0000" + "0000" + "0000000000000000"
            val textLength = "00000000" // 0 length
            val bytes = (uuid + iconUuid + textLength).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 1
            events[0].text shouldBe ""
        }

        should("parse text event with 3-byte text (1 byte padding)") {
            // Text: "abc" (3 bytes, 1 byte padding)
            val uuid = "12345678" + "1234" + "5678" + "0000000000000000"
            val iconUuid = "00000000" + "0000" + "0000" + "0000000000000000"
            val textLength = "03000000" // 3 in little endian
            val textData = "616263" + "00" // "abc" + 1 byte padding
            val bytes = (uuid + iconUuid + textLength + textData).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 1
            events[0].text shouldBe "abc"
        }

        should("parse multiple text events") {
            // Event 1: "Hi"
            val event1Uuid = "11111111" + "1111" + "1111" + "1111111111111111"
            val event1Icon = "22222222" + "2222" + "2222" + "2222222222222222"
            val event1Length = "02000000"
            val event1Text = "4869" + "0000" // "Hi" + 2 bytes padding

            // Event 2: "Bye"
            val event2Uuid = "33333333" + "3333" + "3333" + "3333333333333333"
            val event2Icon = "44444444" + "4444" + "4444" + "4444444444444444"
            val event2Length = "03000000"
            val event2Text = "427965" + "00" // "Bye" + 1 byte padding

            val bytes = (event1Uuid + event1Icon + event1Length + event1Text +
                         event2Uuid + event2Icon + event2Length + event2Text).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 2
            events[0].uuid shouldBe "11111111-1111-1111-1111111111111111"
            events[0].text shouldBe "Hi"
            events[1].uuid shouldBe "33333333-3333-3333-3333333333333333"
            events[1].text shouldBe "Bye"
        }

        should("handle truncated header gracefully") {
            // Only 30 bytes - less than minimum header size of 36
            val partialHeader = "11111111" + "2222" + "3333" + "4444444444444444" + "aaaaaaaa" + "bb"
            val bytes = partialHeader.hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events.shouldBeEmpty()
        }

        should("handle truncated text gracefully") {
            // Header says 10 bytes of text but only 5 are present
            val uuid = "12345678" + "1234" + "5678" + "0000000000000000"
            val iconUuid = "00000000" + "0000" + "0000" + "0000000000000000"
            val textLength = "0a000000" // 10 in little endian
            val textData = "48656c6c6f" // Only 5 bytes
            val bytes = (uuid + iconUuid + textLength + textData).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events.shouldBeEmpty()
        }

        should("strip null terminators from text") {
            // Text with embedded nulls at the end (common in C-style strings)
            val uuid = "12345678" + "1234" + "5678" + "0000000000000000"
            val iconUuid = "00000000" + "0000" + "0000" + "0000000000000000"
            val textLength = "08000000" // 8 bytes
            val textData = "48656c6c6f000000" // "Hello" + 3 null bytes (within the 8)
            val bytes = (uuid + iconUuid + textLength + textData).hexToByteArray()

            val events = Codec.readTextEvents(bytes)

            events shouldHaveSize 1
            events[0].text shouldBe "Hello" // Nulls should be trimmed
        }
    }
})
