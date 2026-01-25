package cz.smarteon.loxkt.event

import cz.smarteon.loxkt.Codec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [Codec.readValueEvents].
 *
 * Value events are 24 bytes each:
 * - UUID: 16 bytes (Data1: 4, Data2: 2, Data3: 2, Data4: 8) - little endian
 * - Value: 8 bytes (double, little endian)
 *
 * Test data can be captured from a real Miniserver using Wireshark or similar tools.
 * Hex strings are used for clarity and easy comparison with protocol documentation.
 */
@OptIn(ExperimentalStdlibApi::class)
class ValueEventTest : ShouldSpec({

    context("readValueEvents") {
        should("return empty list for empty input") {
            Codec.readValueEvents(ByteArray(0)).shouldBeEmpty()
        }

        should("parse single value event") {
            // UUID: 0eed7ea0-0013-0ad8-ffff504f94185cc3
            // Value: 23.5
            //
            // Binary layout (little endian):
            // Data1: a07eed0e (0x0eed7ea0 reversed)
            // Data2: 1300 (0x0013 reversed)
            // Data3: d80a (0x0ad8 reversed)
            // Data4: ffff504f94185cc3 (as-is)
            // Value: 0000000000803740 (23.5 as IEEE 754 double, little endian)
            val hex = "a07eed0e" + "1300" + "d80a" + "ffff504f94185cc3" + "0000000000803740"
            val bytes = hex.hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "0eed7ea0-0013-0ad8-ffff504f94185cc3"
            events[0].value shouldBeExactly 23.5
        }

        should("parse multiple value events") {
            // Event 1: UUID 11111111-2222-3333-4444444444444444, Value: 1.0
            // Event 2: UUID aaaaaaaa-bbbb-cccc-dddddddddddddddd, Value: 2.5
            val event1 = "11111111" + "2222" + "3333" + "4444444444444444" + "000000000000f03f" // 1.0
            val event2 = "aaaaaaaa" + "bbbb" + "cccc" + "dddddddddddddddd" + "0000000000000440" // 2.5
            val bytes = (event1 + event2).hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events shouldHaveSize 2
            events[0].uuid shouldBe "11111111-2222-3333-4444444444444444"
            events[0].value shouldBeExactly 1.0
            events[1].uuid shouldBe "aaaaaaaa-bbbb-cccc-dddddddddddddddd"
            events[1].value shouldBeExactly 2.5
        }

        should("handle zero value") {
            // UUID: 12345678-1234-5678-0000000000000000, Value: 0.0
            val hex = "78563412" + "3412" + "7856" + "0000000000000000" + "0000000000000000"
            val bytes = hex.hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events shouldHaveSize 1
            events[0].value shouldBeExactly 0.0
        }

        should("handle negative value") {
            // UUID: 12345678-1234-5678-0000000000000000, Value: -42.75
            // -42.75 as IEEE 754 double little endian: 00000000006045c0
            val hex = "78563412" + "3412" + "7856" + "0000000000000000" + "00000000006045c0"
            val bytes = hex.hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events shouldHaveSize 1
            events[0].value shouldBeExactly -42.75
        }

        should("handle truncated data gracefully") {
            // Only 20 bytes instead of 24 - should return empty list
            val hex = "78563412" + "3412" + "7856" + "0000000000000000"
            val bytes = hex.hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events.shouldBeEmpty()
        }

        should("parse complete events and ignore trailing incomplete data") {
            // One complete event (24 bytes) + 10 bytes of incomplete second event
            val completeEvent = "11111111" + "2222" + "3333" + "4444444444444444" + "000000000000f03f"
            val incompleteEvent = "aaaaaaaa" + "bbbb" // Only 6 bytes
            val bytes = (completeEvent + incompleteEvent).hexToByteArray()

            val events = Codec.readValueEvents(bytes)

            events shouldHaveSize 1
            events[0].uuid shouldBe "11111111-2222-3333-4444444444444444"
        }
    }
})
